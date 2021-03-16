package bio.terra.cli.service.utils;

import bio.terra.cli.command.exception.InternalErrorException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Google Cloud Storage endpoints. */
public class GoogleCloudStorage {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

  // the google project id of the workspace
  private final String googleProjectId;

  // the client object used for talking to Google Cloud Storage
  private final Storage storageClient;

  /**
   * Constructor for class that talks to Google Cloud Storage. Methods in this class will use the
   * given credentials to call authenticated endpoints.
   *
   * <p>This constructor scopes Storage requests to the given project.
   *
   * @param googleCredentials the credentials that will be used to call authenticated endpoints
   * @param googleProjectId scope Storage requests to this project
   */
  public GoogleCloudStorage(GoogleCredentials googleCredentials, String googleProjectId) {
    this.googleProjectId = googleProjectId;
    this.storageClient = buildClient(googleCredentials);
  }

  /**
   * Build the GCS client object for the given credentials and project.
   *
   * @return the GCS client object
   */
  private Storage buildClient(GoogleCredentials googleCredentials) {
    StorageOptions.Builder storageOptions = StorageOptions.newBuilder();
    if (googleProjectId != null) {
      storageOptions.setProjectId(googleProjectId);
    }

    // set the credentials to use when talking to GCS
    storageOptions.setCredentials(googleCredentials);

    return storageOptions.build().getService();
  }

  /**
   * Create a new GCS bucket.
   *
   * @param bucketName name of the bucket
   * @return the bucket object
   */
  public Bucket createBucket(String bucketName) {
    logger.info("Creating bucket: {}", bucketName);

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
    logger.info("Deleting bucket: {}", bucketUri);
    String bucketName = bucketUri.replaceFirst("^gs://", "");

    boolean deleted = storageClient.delete(bucketName);
    if (!deleted) {
      throw new InternalErrorException("Bucket deletion failed.");
    }
  }

  /**
   * Check whether we have permission to list objects in the bucket.
   *
   * <p>List access is included in the Storage Object Viewer and Storage Legacy Bucket Reader. It is
   * NOT included in the Storage Legacy Object Reader.
   *
   * @param bucketUri uri of the bucket (gs://...)
   * @return true if access is allowed
   */
  public boolean checkObjectsListAccess(String bucketUri) {
    try {
      String bucketName = bucketUri.replaceFirst("^gs://", "");

      // try listing objects in the bucket
      storageClient.list(
          bucketName,
          Storage.BlobListOption.userProject(googleProjectId),
          Storage.BlobListOption.fields());
      return true;
    } catch (StorageException storageEx) {
      logger.debug("Storage exception http code = {}", storageEx.getCode(), storageEx);
      if (storageEx.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND
          || storageEx.getCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN) {
        return false;
      }
      throw storageEx;
    }
  }
}
