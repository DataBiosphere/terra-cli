package bio.terra.cli.service.utils;

import bio.terra.cli.context.TerraUser;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Google Cloud Storage endpoints. */
public class GoogleCloudStorage {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

  // the Terra user whose credentials will be used to call authenticated requests
  private final TerraUser terraUser;

  // the google project id of the workspace
  private final String googleProjectId;

  // the client object used for talking to Google Cloud Storage
  private Storage storageClient;

  /**
   * Constructor for class that talks to Google Cloud Storage. The user must be authenticated.
   * Methods in this class will use its credentials to call authenticated endpoints.
   *
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  public GoogleCloudStorage(TerraUser terraUser, String googleProjectId) {
    this.terraUser = terraUser;
    this.googleProjectId = googleProjectId;
    this.storageClient = null;
    buildClientForTerraUser();
  }

  /**
   * Build the GCS client object for the given Terra user. If terraUser is null, this method builds
   * the client object without an access token set.
   */
  private void buildClientForTerraUser() {
    StorageOptions.Builder storageOptions = StorageOptions.newBuilder();
    storageOptions.setProjectId(googleProjectId);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      storageOptions.setCredentials(terraUser.userCredentials);
    }
    this.storageClient = storageOptions.build().getService();
  }

  /**
   * Create a new GCS bucket.
   *
   * @param bucketName name of the bucket
   * @return the bucket object
   */
  public Bucket createBucket(String bucketName) {
    logger.info("creating bucket: {}", bucketName);

    // TODO: optionally set lifecycle rules here
    BucketInfo bucketInfo = BucketInfo.newBuilder(bucketName).build();
    return storageClient.create(bucketInfo);
  }

  /**
   * Delete an existing GCS bucket.
   *
   * @param bucketUri uri of the bucket (gs://...)
   */
  public void deleteBucket(String bucketUri) {
    logger.info("deleting bucket: {}", bucketUri);
    String bucketName = bucketUri.replaceFirst("^gs://", "");

    boolean deleted = storageClient.delete(bucketName);
    if (!deleted) {
      throw new RuntimeException("Bucket deletion failed.");
    }
  }
}
