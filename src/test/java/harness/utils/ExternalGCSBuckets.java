package harness.utils;

import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Group;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.BucketInfo.IamConfiguration;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;
import harness.CRLJanitor;
import harness.TestExternalResources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.apache.http.HttpStatus;

/**
 * Utility methods for creating external GCS buckets for testing workspace references. Most methods
 * in the class use the CRL wrapper around the GCS client library to manipulate external buckets.
 * This class also includes a method to build the unwrapped GCS client object, which is what we
 * would expect users to call. Setup/cleanup of external buckets should use the CRL wrapper.
 * Fetching information from the cloud as a user may do should use the unwrapped client object.
 */
public class ExternalGCSBuckets {

  public static BucketInfo createBucketWithFineGrainedAccess() throws IOException {
    return createBucket(/*isUniformBucketLevelAccessEnabled=*/ false);
  }

  public static BucketInfo createBucketWithUniformAccess() throws IOException {
    return createBucket(/*isUniformBucketLevelAccessEnabled=*/ true);
  }

  /**
   * Create a bucket in an external project. This is helpful for testing referenced GCS bucket
   * resources. This method uses SA credentials that have permissions in the external (to WSM)
   * project.
   */
  private static BucketInfo createBucket(boolean isUniformBucketLevelAccessEnabled)
      throws IOException {
    String bucketName = UUID.randomUUID().toString();
    StorageClass storageClass = StorageClass.STANDARD;
    String location = "US";

    BucketCow bucket =
        getStorageCow()
            .create(
                BucketInfo.newBuilder(bucketName)
                    .setStorageClass(storageClass)
                    .setLocation(location)
                    .setIamConfiguration(
                        IamConfiguration.newBuilder()
                            .setIsUniformBucketLevelAccessEnabled(isUniformBucketLevelAccessEnabled)
                            .build())
                    .build());

    BucketInfo bucketInfo = bucket.getBucketInfo();
    System.out.println(
        "Created bucket "
            + bucketInfo.getName()
            + " in "
            + bucketInfo.getLocation()
            + " with storage class "
            + bucketInfo.getStorageClass()
            + " in project "
            + TestExternalResources.getProjectId());
    return bucketInfo;
  }

  /**
   * Delete a bucket in an external project. This method uses SA credentials that have permissions
   * in the external (to WSM) project.
   */
  public static void deleteBucket(BucketInfo bucketInfo) throws IOException {
    getStorageCow().delete(bucketInfo.getName());
  }

  /**
   * Grant a given user or group object viewer access to a bucket. This method uses SA credentials
   * to set IAM policy on a bucket in an external (to WSM) project.
   */
  public static void grantReadAccess(BucketInfo bucketInfo, Identity userOrGroup)
      throws IOException {
    grantAccess(bucketInfo, userOrGroup, StorageRoles.objectViewer());
  }

  /**
   * Grant a given user or group admin access to a bucket. This method uses SA credentials to set
   * IAM policy on a bucket in an external (to WSM) project.
   */
  public static void grantWriteAccess(BucketInfo bucketInfo, Identity userOrGroup)
      throws IOException {
    grantAccess(bucketInfo, userOrGroup, StorageRoles.admin());
  }

  /**
   * Helper method to grant a given user or group roles on a bucket. This method uses SA credentials
   * to set IAM policy on a bucket in an external (to WSM) project.
   */
  private static void grantAccess(BucketInfo bucketInfo, Identity userOrGroup, Role... roles)
      throws IOException {
    StorageCow storage = getStorageCow();
    Policy currentPolicy = storage.getIamPolicy(bucketInfo.getName());
    Policy.Builder updatedPolicyBuilder = currentPolicy.toBuilder();
    for (Role role : roles) {
      updatedPolicyBuilder.addIdentity(role, userOrGroup);
    }
    storage.setIamPolicy(bucketInfo.getName(), updatedPolicyBuilder.build());
  }

  /** Grant a user role on a gcs file, a.k.a set the {@link Acl} of the given {@link BlobId}. */
  public static void grantAccess(
      String bucketName, String blobName, Group group, com.google.cloud.storage.Acl.Role role)
      throws IOException {
    StorageCow storage = getStorageCow();
    storage.updateAcl(bucketName, Acl.of(group, role));
    BlobId blobId = BlobId.of(bucketName, blobName);
    storage.createAcl(blobId, Acl.of(group, role));
  }

  /** Utility method to write an arbitrary blob to a bucket. */
  public static void writeBlob(GoogleCredentials credentials, String bucketName, String blobName)
      throws InterruptedException {
    Storage storageClient = getStorageClient(credentials);
    BucketInfo bucket = storageClient.get(bucketName);

    BlobId blobId = BlobId.of(bucket.getName(), blobName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    byte[] blobData = "test blob data".getBytes(StandardCharsets.UTF_8);

    // retry forbidden errors because we often see propagation delays when a user is just granted
    // access
    HttpUtils.callWithRetries(
        () -> {
          storageClient.create(blobInfo, blobData);
          return null;
        },
        (ex) ->
            (ex instanceof StorageException)
                && ((StorageException) ex).getCode() == HttpStatus.SC_FORBIDDEN,
        5,
        Duration.ofMinutes(1));
  }

  /**
   * Helper method to build the CRL wrapper around the GCS client object with SA credentials that
   * have permissions on the external (to WSM) project.
   */
  private static StorageCow getStorageCow() throws IOException {
    StorageOptions options =
        StorageOptions.newBuilder()
            .setCredentials(TestExternalResources.getSACredentials())
            .setProjectId(TestExternalResources.getProjectId())
            .build();
    return new StorageCow(CRLJanitor.DEFAULT_CLIENT_CONFIG, options);
  }

  /**
   * Helper method to build the GCS client object with the given credentials. Note this is not
   * wrapped by CRL because this is what we expect most users to use.
   */
  public static Storage getStorageClient(GoogleCredentials credentials) {
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

  public static String getGsPath(String bucketName, String filePath) {
    return getGsPath(bucketName) + "/" + filePath;
  }
}
