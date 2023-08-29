package bio.terra.cli.service;

import static org.slf4j.LoggerFactory.*;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;

public class FeatureService {

  private static final Logger LOGGER = getLogger(FeatureService.class);
  private final FlagsmithClient flagsmith;

  private FeatureService(Server server) {
    if (!TextUtils.isEmpty(server.getFlagsmithApiUrl())) {
      flagsmith = FlagsmithClient
          .newBuilder()
          .withApiUrl(server.getFlagsmithApiUrl())
          .setApiKey(server.getFlagsmithClientSideKey())
          .build();
    } else {
      flagsmith = null;
    }
  }

  public static FeatureService fromContext() {
    return new FeatureService(Context.getServer());
  }

  public Optional<Boolean> isFeatureEnabled(String feature) {
    return isFeatureEnabled(feature, /*userEmail=*/null);
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   *
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature, @Nullable String userEmail) {
    if (flagsmith == null) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }
    try {
        return Optional.of(getFlags(flagsmith, userEmail).isFeatureEnabled(feature));
      } catch (Exception e) {
        LOGGER.debug("failed to fetch feature flag value");
        return Optional.empty();
      }
  }

  private static Flags getFlags(FlagsmithClient flagsmith, String userEmail)
      throws FlagsmithClientError {
    if (userEmail == null) {
      return flagsmith.getEnvironmentFlags();
    }
    Map<String, Object> traits = new HashMap<>();
    traits.put("email_address", userEmail);
    return flagsmith.getIdentityFlags(userEmail, traits);
  }


}
