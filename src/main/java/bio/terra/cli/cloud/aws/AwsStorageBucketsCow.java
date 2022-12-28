package bio.terra.cli.cloud.aws;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.AwsCredential;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.sagemaker.endpoints.internal.Value.Str;

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

  public byte[] get(String bucketName, String bucketPrefix, boolean isFolder) { // todo-206
    try {
      String objectKey = bucketPrefix;
      if (isFolder) {
        objectKey += "/";
      }

      GetObjectResponse getResponse =
          bucketsClient
              .getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(objectKey).build())
              .response();

      SdkHttpResponse httpResponse = getResponse.sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error getting storage bucket, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }

      if (getResponse.deleteMarker() != null && getResponse.deleteMarker()) {
        throw new UserActionableException("Cannot access storage bucket marked for deletion");
      }

      getResponse


      logger.error("TEST 1 --> " + test.asByteArray());
      // return TODO
      return test.response().toString();
    } catch (Exception e) {
      checkException(e);
      throw new SystemException("Error getting storage bucket " + e.getClass().getName(), e);
    }
  }

  /**
   * Returns the number of objects in the bucket, up to the given limit, or null if there was an
   * error looking it up. This behavior is useful for display purposes.
   */
  public Integer getNumObjects(String bucketName, String bucketPrefix, long limit) {
    /*
    numObjects: not supported for AWS
    Terra bucket -> S3://<bucketName>/<bucketPrefix>
    <bucketName> is shared across workspaces. Hence 's3:ListBucket' is not permitted
    Subsequently objects with <bucketPrefix> cannot be listed / counted
     */
    throw new UnsupportedOperationException(
        "Operation GetNumObjects not supported on platform AWS");
  }

  public void checkException(Exception ex) {
    if (ex instanceof NoSuchKeyException
        || (ex instanceof SdkException && ex.getMessage().contains("Access Denied"))) {
      throw new UserActionableException(
          "Error accessing storage bucket, check the bucket name / permissions and retry");
    } else if (ex instanceof InvalidObjectStateException) {
      throw new UserActionableException("Cannot access archived storage bucket until restored");
    }
  }
}
