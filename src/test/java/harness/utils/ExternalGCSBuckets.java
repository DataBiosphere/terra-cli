package harness.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;
import harness.TestExternalResources;
import java.io.IOException;
import java.util.UUID;

/** Utility methods for creating external GCS buckets for testing workspace references. */
public class ExternalGCSBuckets {
  /**
   * Create a bucket in an external project. This is helpful for testing referenced GCS bucket
   * resources. This method uses SA credentials for an external project.
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
            + TestExternalResources.getProjectId());
    return bucket;
  }

  /**
   * Grant a given user object viewer access to a bucket. This method uses SA credentials for an
   * external project.
   */
  public static void grantReadAccess(Bucket bucket, String email) throws IOException {
    grantAccess(
        bucket,
        Identity.user(email),
        // TODO (PF-717): revisit this once we're calling WSM endpoints for check-access
        StorageRoles.objectViewer(),
        StorageRoles.legacyBucketReader());
  }

  /**
   * Grant a given user admin access to a bucket. This method uses SA credentials for an external
   * project.
   */
  public static void grantWriteAccess(Bucket bucket, Identity user) throws IOException {
    grantAccess(bucket, user, StorageRoles.admin());
  }

  /**
   * Helper method to grant a given user roles on a bucket. This method uses SA credentials for an
   * external project.
   */
  private static void grantAccess(Bucket bucket, Identity user, Role... roles) throws IOException {
    Storage storage = getStorageClient();
    Policy currentPolicy = storage.getIamPolicy(bucket.getName());
    Policy.Builder updatedPolicyBuilder = currentPolicy.toBuilder();
    for (Role role : roles) {
      updatedPolicyBuilder.addIdentity(role, user);
    }
    Policy updatedPolicy = storage.setIamPolicy(bucket.getName(), updatedPolicyBuilder.build());
    storage.setIamPolicy(bucket.getName(), updatedPolicy);
  }

  /** Helper method to build the GCS client object with SA credentials for the external project. */
  public static Storage getStorageClient() throws IOException {
    return getStorageClient(TestExternalResources.getSACredentials());
  }

  /** Helper method to build the GCS client object with the given credentials. */
  public static Storage getStorageClient(GoogleCredentials credentials) throws IOException {
    return StorageOptions.newBuilder()
        .setProjectId(TestExternalResources.getProjectId())
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
