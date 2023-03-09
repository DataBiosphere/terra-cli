package bio.terra.cli.cloud.aws;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.AwsCredential;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/** A Cloud Object Wrapper(COW) for AWS S3Client Library: {@link S3Client} */
public class AwsStorageBucketsCow {
  private static final Duration AWS_STORAGE_BUCKET_WAITER_TIMEOUT_DURATION =
      Duration.ofSeconds(900);
  private final Logger logger = LoggerFactory.getLogger(AwsStorageBucketsCow.class);
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
                      .waitTimeout(AWS_STORAGE_BUCKET_WAITER_TIMEOUT_DURATION)
                      .build())
              .build());
    } catch (Exception e) {
      throw new SystemException("Error creating bucketsClient client.", e);
    }
  }

  public Optional<AwsS3Blob> getBlob(String bucketName, String bucketPrefix) {
    if (bucketPrefix.endsWith("/")) {
      throw new UserActionableException("Get blob operation not supported on folder objects.");
    }

    try {
      ResponseInputStream<GetObjectResponse> getResponseStream =
          bucketsClient.getObject(
              GetObjectRequest.builder().bucket(bucketName).key(bucketPrefix).build());

      GetObjectResponse getResponse = getResponseStream.response();
      SdkHttpResponse httpResponse = getResponse.sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error getting storage bucket, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }

      if (getResponse.deleteMarker() != null && getResponse.deleteMarker()) {
        throw new UserActionableException("Cannot access storage bucket marked for deletion");
      }

      return Optional.of(
          new AwsS3Blob(getResponseStream.readAllBytes(), getResponse.contentType()));

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
    try {
      Iterator<ListObjectsV2Response> listIterator =
          bucketsClient
              .listObjectsV2Paginator(
                  ListObjectsV2Request.builder()
                      .bucket(bucketName)
                      .prefix(bucketPrefix + "/")
                      .maxKeys(AwsClient.AWS_CLIENT_MAXIMUM_RESULTS_PER_CALL)
                      .build())
              .iterator();

      int numObjectsCtr = 0;
      while (listIterator.hasNext() && (numObjectsCtr < limit)) {
        numObjectsCtr +=
            listIterator.next().contents().stream()
                .filter(s3Object -> !s3Object.key().endsWith("/"))
                .limit(limit - numObjectsCtr)
                .count();
      }
      return numObjectsCtr;

    } catch (Exception e) {
      checkException(e);
      throw new SystemException("Error getting storage bucket " + e.getClass().getName(), e);
    }
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

  public record AwsS3Blob(byte[] bytes, String encoding) {
    public String print() throws UnsupportedEncodingException {
      try {
        return new String(bytes, encoding);
      } catch (UnsupportedEncodingException e) {
        throw new UnsupportedEncodingException(
            "Error getting object contents - unsupported content type");
      }
    }
  }
}
