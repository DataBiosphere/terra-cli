package harness.utils;

import static harness.TestExternalResources.getProjectId;
import static harness.TestExternalResources.getSACredentials;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;
import java.io.IOException;
import java.util.UUID;

/** Utility methods for creating external GCS buckets for testing workspace references. */
public class ExternalGCSBuckets {
  /**
   * Get a bucket. This is helpful for testing controlled GCS bucket resources. It allows tests to
   * check metadata that is not stored in WSM, only in GCS. This method takes in the credentials to
   * use because tests typically want to check metadata as the test user.
   */
  public static Bucket getBucket(String bucketName, GoogleCredentials credentials)
      throws IOException {
    return getStorageClient(credentials).get(bucketName);
  }

  /**
   * Create a bucket in the external project. This is helpful for testing referenced GCS bucket
   * resources. This method uses SA credentials for the external project.
   */
  public static Bucket createBucket() throws IOException {
    String bucketName = UUID.randomUUID().toString();
    StorageClass storageClass = StorageClass.STANDARD;
    String location = "US";

    Bucket bucket =
        getStorageClient()
            .create(
                BucketInfo.newBuilder(bucketName)
                    .setStorageClass(storageClass)
                    .setLocation(location)
                    .build());

    System.out.println(
        "Created bucket "
            + bucket.getName()
            + " in "
            + bucket.getLocation()
            + " with storage class "
            + bucket.getStorageClass()
            + " in project "
            + getProjectId());
    return bucket;
  }

  /**
   * Delete a bucket in the external project. This is helpful for testing referenced GCS bucket
   * resources. This method uses SA credentials for the external project.
   */
  public static void deleteBucket(Bucket bucket) throws IOException {
    getStorageClient().delete(bucket.getName());
  }

  /**
   * Grant a given user object viewer access to a bucket. This method uses SA credentials for the
   * external project.
   */
  public static void grantReadAccess(Bucket bucket, String email) throws IOException {
    Storage storage = getStorageClient();
    Policy currentPolicy = storage.getIamPolicy(bucket.getName());
    Policy updatedPolicy =
        storage.setIamPolicy(
            bucket.getName(),
            currentPolicy
                .toBuilder()
                // TODO (PF-717): revisit this once we're calling WSM endpoints for check-access
                .addIdentity(StorageRoles.objectViewer(), Identity.user(email))
                .addIdentity(StorageRoles.legacyBucketReader(), Identity.user(email))
                .build());
    getStorageClient().setIamPolicy(bucket.getName(), updatedPolicy);
  }

  /** Helper method to build the GCS client object with SA credentials for the external project. */
  private static Storage getStorageClient() throws IOException {
    return getStorageClient(getSACredentials());
  }

  /** Helper method to build the GCS client object with the given credentials. */
  private static Storage getStorageClient(GoogleCredentials credentials) throws IOException {
    return StorageOptions.newBuilder()
        .setProjectId(getProjectId())
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
