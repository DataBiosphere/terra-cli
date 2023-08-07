package bio.terra.cli.service;

import static org.slf4j.LoggerFactory.*;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.time.Duration;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;

public class FeatureService {

  private static final Logger LOGGER = getLogger(FeatureService.class);

  private static final Duration DEFAULT_RETRY_SLEEP_DURATION = Duration.ofSeconds(10);

  private static final int MAX_RETRY = 3;

  private final Server server;

  private FeatureService(Server server) {
    this.server = server;
  }

  public static FeatureService fromContext() {
    return new FeatureService(Context.getServer());
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   *
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature) {
    if (TextUtils.isEmpty(server.getFlagsmithApiUrl())) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }
    try {
      HttpResponse<JsonNode> jsonResponse = retryGet(() -> getFeatureFlagValue(feature));
      if (HttpStatus.SC_OK == jsonResponse.getStatus()) {
        return Optional.of(jsonResponse.getBody().getObject().getBoolean("enabled"));
      } else {
        LOGGER.info("GET feature flag failed with status: " + jsonResponse.getStatusText());
      }
    } catch (Exception e) {
      LOGGER.debug("Failed to fetch feature flag value", e);
    }
    return Optional.empty();
  }

  private HttpResponse<JsonNode> getFeatureFlagValue(String feature) {
    try {
      return Unirest.get(server.getFlagsmithApiUrl() + "flags/")
          .header("accept", "application/json")
          .header("X-Environment-Key", server.getFlagsmithClientSideKey())
          .queryString("feature", feature)
          .asJson();
    } catch (UnirestException e) {
      throw new SystemException("Fail to get state for feature: " + feature);
    }
  }

  private static HttpResponse<JsonNode> retryGet(
      SupplierWithException<HttpResponse<JsonNode>> requestSupplier) throws Exception {
    HttpResponse<JsonNode> response = requestSupplier.get();
    for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
      try {
        if (response.getStatus() == 200) {
          // Successful response, return the result
          return response;
        }

        if (isServerError(response.getStatus())) {
          LOGGER.debug("Attempt " + attempt + ": Server error, retry in 10 seconds");
          Thread.sleep(DEFAULT_RETRY_SLEEP_DURATION.toMillis());
          response = requestSupplier.get();
        } else {
          // If it's neither 200 nor 500, some other error occurred, stop retrying
          LOGGER.debug("Attempt " + attempt + ": Error occurred, stopping retrying.");
          return response;
        }
      } catch (InterruptedException e) {
        throw new SystemException("Failed during retry", e);
      }
    }

    // Max attempts reached, return the last response received
    LOGGER.debug(
        "Max attempts reached. Last response received with status code "
            + response.getStatus()
            + ".");
    return response;
  }

  private static boolean isServerError(int statusCode) {
    return statusCode >= 500 && statusCode <= 599;
  }

  /**
   * Supplier that can throw
   *
   * @param <T> return type for the non-throw case
   */
  @FunctionalInterface
  private interface SupplierWithException<T> {
    T get() throws Exception;
  }
}
