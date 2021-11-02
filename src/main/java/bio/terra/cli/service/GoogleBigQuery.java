package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.TableList;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to Google BigQuery. */
public class GoogleBigQuery {
  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQuery.class);

  private final BigQueryCow bigQuery;

  // default value for the maximum number of times to retry HTTP requests to BQ
  public static final int BQ_MAXIMUM_RETRIES = 5;

  /**
   * Factory method for class that talks to BQ. Pulls the current user from the context. Uses the
   * pet SA credentials instead of the end user credentials because we need the cloud-platform scope
   * to talk to the cloud directly. The CLI does not request the cloud-platform scope during the
   * user login flow, so we need to use the pet SA credentials instead when that scope is needed.
   */
  public static GoogleBigQuery fromContextForPetSa() {
    return new GoogleBigQuery(Context.requireUser().getPetSACredentials());
  }

  private GoogleBigQuery(GoogleCredentials credentials) {
    bigQuery = CrlUtils.createBigQueryCow(credentials);
  }

  public Optional<Dataset> getDataset(String projectId, String datasetId) {
    try {
      Dataset dataset =
          callWithRetries(
              () -> bigQuery.datasets().get(projectId, datasetId).execute(),
              "Error looking up dataset.");
      return Optional.ofNullable(dataset);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public long getNumTables(String projectId, String datasetId) {
    try {
      TableList tables =
          callWithRetries(
              () -> bigQuery.tables().list(projectId, datasetId).execute(),
              "Error looking up dataset tables.");
      return tables.getTotalItems();
    } catch (Exception ex) {
      return 0;
    }
  }

  /**
   * Execute a function that includes hitting BQ endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the BQ client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the BQ client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                GoogleBigQuery::isRetryable,
                BQ_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting BQ endpoints. If an exception is thrown by the BQ
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the BQ client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (IOException | InterruptedException ex) {
      // wrap the BQ exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  /**
   * Utility method that checks if an exception thrown by the BQ client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  static boolean isRetryable(Exception ex) {
    if (!(ex instanceof GoogleJsonResponseException)) {
      return false;
    }
    logger.error("Caught a BQ error.", ex);
    int statusCode = ((GoogleJsonResponseException) ex).getStatusCode();

    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT

        // retry forbidden errors because we often see propagation delays when a user is just
        // granted access
        || statusCode == HttpStatus.SC_FORBIDDEN;
  }
}
