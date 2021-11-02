package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cloudres.google.storage.BlobCow;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageException;
import java.util.Iterator;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to Google Cloud Storage. */
public class GoogleCloudStorage {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

  private final StorageCow storage;

  // default value for the maximum number of times to retry HTTP requests to GCS
  public static final int GCS_MAXIMUM_RETRIES = 5;

  /**
   * Factory method for class that talks to GCS. Pulls the current user from the context. Uses the
   * pet SA credentials instead of the end user credentials because we need the cloud-platform scope
   * to talk to the cloud directly. The CLI does not request the cloud-platform scope during the
   * user login flow, so we need to use the pet SA credentials instead when that scope is needed.
   */
  public static GoogleCloudStorage fromContextForPetSa() {
    return new GoogleCloudStorage(Context.requireUser().getPetSACredentials());
  }

  private GoogleCloudStorage(GoogleCredentials credentials) {
    storage = CrlUtils.createStorageCow(credentials);
  }

  public Optional<BucketCow> getBucket(String bucketName) {
    try {
      BucketCow bucketCow =
          callWithRetries(() -> storage.get(bucketName), "Error looking up bucket.");
      return Optional.of(bucketCow);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Returns the number of objects in the bucket, or null if there was an error looking it up. This
   * behavior is useful for display purposes.
   */
  public Integer getNumObjects(BucketCow bucket) {
    try {
      Page<BlobCow> objList =
          callWithRetries(() -> bucket.list(), "Error looking up objects in bucket.");
      Iterator<BlobCow> objItr = objList.getValues().iterator();

      int numObjectsCtr = 0;
      while (objItr.hasNext()) {
        numObjectsCtr++;
        objItr.next();
      }
      return numObjectsCtr;
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Execute a function that includes hitting GCS endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the GCS client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the GCS client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, StorageException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                GoogleCloudStorage::isRetryable,
                GCS_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting GCS endpoints. If an exception is thrown by the GCS
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the GCS client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, StorageException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (StorageException | InterruptedException ex) {
      // wrap the GCS exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  /**
   * Utility method that checks if an exception thrown by the GCS client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  static boolean isRetryable(Exception ex) {
    if (!(ex instanceof StorageException)) {
      return false;
    }
    logger.error("Caught a GCS error.", ex);
    int statusCode = ((StorageException) ex).getCode();

    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT

        // retry forbidden errors because we often see propagation delays when a user is just
        // granted access
        || statusCode == HttpStatus.SC_FORBIDDEN;
  }
}
