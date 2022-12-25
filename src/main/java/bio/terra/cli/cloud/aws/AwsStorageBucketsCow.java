package bio.terra.cli.cloud.aws;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.AwsCredential;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/** A Cloud Object Wrapper(COW) for AWS S3Client Library: {@link S3Client} */
public class AwsStorageBucketsCow {
  private static final int AWS_CLIENT_MAXIMUM_RETRIES = 5;
  private static final Duration AWS_STORAGE_BUCKET_WAITER_TIMEOUT_DURATION =
      Duration.ofSeconds(900);
  private static final Logger logger = LoggerFactory.getLogger(AwsStorageBucketsCow.class);
  private final S3Client bucketsClient;
  private final S3Waiter bucketsWaiter;

  public AwsStorageBucketsCow(S3Client bucketsClient, S3Waiter bucketsWaiter) {
    this.bucketsClient = bucketsClient;
    this.bucketsWaiter = bucketsWaiter;
  }

  /** Create a {@link AwsStorageBucketsCow} with some default configurations for convenience. */
  public static AwsStorageBucketsCow create(AwsCredential awsCredential, String location) {
    try {
      S3Client bucketsClient =
          S3Client.builder()
              .region(Region.of(location))
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsSessionCredentials.create(
                          awsCredential.getAccessKeyId(),
                          awsCredential.getSecretAccessKey(),
                          awsCredential.getSessionToken())))
              .build();
      return new AwsStorageBucketsCow(
          bucketsClient,
          S3Waiter.builder()
              .client(bucketsClient)
              .overrideConfiguration(
                  WaiterOverrideConfiguration.builder()
                      .maxAttempts(AWS_CLIENT_MAXIMUM_RETRIES)
                      .waitTimeout(AWS_STORAGE_BUCKET_WAITER_TIMEOUT_DURATION)
                      .build())
              .build());
    } catch (Exception e) {
      throw new SystemException("Error creating bucketsClient client.", e);
    }
  }

  public String get(String bucketName, String bucketPrefix) {
    try {
      GetObjectResponse getResponse =
          bucketsClient
              .getObject(GetObjectRequest.builder().bucket(bucketName).key(bucketPrefix).build())
              .response();

      SdkHttpResponse httpResponse = getResponse.sdkHttpResponse();

      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error getting storage bucket, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      // return TODO
      logger.debug("TEST --> " + getResponse);
      return getResponse.toString();

    } catch (Exception e) {
      checkException(e);
      throw new SystemException("Error getting storage bucket", e);
    }
  }

  public void checkException(Exception ex) {
    if (ex instanceof NoSuchKeyException) {
      throw new UserActionableException(
          "Error accessing storage bucket, check the bucket name / permissions and retry");
    } else if (ex instanceof InvalidObjectStateException) {
      throw new UserActionableException("Cannot access archived storage bucket until restored");
    }
  }

  /**
   * Returns the number of objects in the bucket, up to the given limit, or null if there was an
   * error looking it up. This behavior is useful for display purposes. / public Integer
   * getNumObjects(BucketCow bucket, long limit) { try { Page<BlobCow> objList = callWithRetries( ()
   * -> bucket.list(Storage.BlobListOption.pageSize(limit)), "Error looking up objects in bucket.");
   * Iterator<BlobCow> objItr = objList.getValues().iterator();
   *
   * <p>int numObjectsCtr = 0; while (objItr.hasNext()) { numObjectsCtr++; objItr.next(); } return
   * numObjectsCtr; } catch (Exception ex) { return null; } }
   */

  /**
   * Utility method that checks if an exception thrown by the AWS client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  /*
   static boolean isRetryable(Exception ex) {
     if (ex instanceof SocketTimeoutException) {
       return true;
     }
     if (!(ex instanceof StorageException)) {
       return false;
     }
     logger.error("Caught a AWS error.", ex);
     int statusCode = ((StorageException) ex).getCode();

     return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
         || statusCode == HttpStatus.SC_BAD_GATEWAY
         || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
         || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT

         // retry forbidden errors because we often see propagation delays when a user is just
         // granted access
         || statusCode == HttpStatus.SC_FORBIDDEN;
   }

   public Optional<BlobCow> getBlob(String bucketName, String blobPath) {
     try {
       BlobId blobId = BlobId.of(bucketName, blobPath);
       BlobCow blobCow = callWithRetries(() -> storage.get(blobId), "Error looking up blob.");
       return Optional.ofNullable(blobCow);
     } catch (Exception e) {
       return Optional.empty();
     }
   }

  */

  /**
   * Execute a function that includes hitting AWS endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the AWS client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the AWS client or the retries
   */
  /* private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, StorageException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                AwsStorageBucketsCow::isRetryable,
                AWS_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  } */

  /**
   * Execute a function that includes hitting AWS endpoints. If an exception is thrown by the AWS
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the AWS client or the retries
   */
  /* private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, StorageException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (StorageException | InterruptedException ex) {
      // wrap the AWS exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }*/
}
