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
   * resources. This method uses SA credentials to set IAM policy on a bucket in an external (to
   * WSM) project.
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
   * Grant a given user or group object viewer access to a bucket. This method uses SA credentials
   * to set IAM policy on a bucket in an external (to WSM) project.
   */
  public static void grantReadAccess(Bucket bucket, Identity userOrGroup) throws IOException {
    grantAccess(bucket, userOrGroup, StorageRoles.objectViewer());
  }

  /**
   * Grant a given user or group admin access to a bucket. This method uses SA credentials to set
   * IAM policy on a bucket in an external (to WSM) project.
   */
  public static void grantWriteAccess(Bucket bucket, Identity userOrGroup) throws IOException {
    grantAccess(bucket, userOrGroup, StorageRoles.admin());
  }

  /**
   * Helper method to grant a given user or group roles on a bucket. This method uses SA credentials
   * to set IAM policy on a bucket in an external (to WSM) project.
   */
  private static void grantAccess(Bucket bucket, Identity userOrGroup, Role... roles)
      throws IOException {
    Storage storage = getStorageClient();
    Policy currentPolicy = storage.getIamPolicy(bucket.getName());
    Policy.Builder updatedPolicyBuilder = currentPolicy.toBuilder();
    for (Role role : roles) {
      updatedPolicyBuilder.addIdentity(role, userOrGroup);
    }
    storage.setIamPolicy(bucket.getName(), updatedPolicyBuilder.build());
  }

  /** Helper method to build the GCS client object with SA credentials. */
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

  /** Utility method to get the gs:// path of a bucket. */
  public static String getGsPath(String bucketName) {
    return "gs://" + bucketName;
  }
}
