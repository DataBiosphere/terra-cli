package harness.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Utility methods for creating external GCS buckets for testing workspace references. */
public class ExternalGCSBuckets {
  public static final String gcpProjectId = "terra-cli-dev";

  private static final String saKeyFile = "./rendered/ci-account.json";
  private static final List<String> cloudPlatformScope =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

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
            + gcpProjectId);
    return bucket;
  }

  /**
   * Grant a given user object viewer access to a bucket. This method uses SA credentials for an
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

  /** Helper method to build the GCS client object with SA credentials for an external project. */
  public static Storage getStorageClient() throws IOException {
    GoogleCredentials saCredentials =
        ServiceAccountCredentials.fromStream(new FileInputStream(saKeyFile))
            .createScoped(cloudPlatformScope);
    return getStorageClient(saCredentials);
  }

  /** Helper method to build the GCS client object with the given credentials. */
  public static Storage getStorageClient(GoogleCredentials credentials) throws IOException {
    return StorageOptions.newBuilder()
        .setProjectId(gcpProjectId)
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
