package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import com.google.cloud.Identity;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for including the number of objects in a GCS bucket resource's description. */
@Tag("unit")
public class GcsBucketNumObjects extends SingleWorkspaceUnit {
  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blob in external bucket
  private String externalBucketBlobName = "testBlob";

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucket();
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.user(workspaceCreator.email));

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(Auth.getProxyGroupEmail()));

    // upload a file to the bucket
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        externalBucketBlobName);
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();

    // need to delete all the objects in the bucket before we can delete the bucket
    try {
      Storage storageClient =
          ExternalGCSBuckets.getStorageClient(
              workspaceCreator.getCredentialsWithCloudPlatformScope());
      BlobId blobId = BlobId.of(externalBucket.getName(), externalBucketBlobName);
      storageClient.delete(blobId);
    } catch (IOException ioEx) {
      System.out.println("Error deleting objects in the external bucket.");
      ioEx.printStackTrace();
    }

    ExternalGCSBuckets.deleteBucket(externalBucket);
    externalBucket = null;
  }

  @Test
  @DisplayName("controlled bucket displays the number of objects")
  void numObjectsForControlled() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "numObjectsForControlled";
    String bucketName = UUID.randomUUID().toString();
    UFGcsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + bucketName);

    // check that there are initially 0 objects reported in the bucket
    assertEquals(0, createdBucket.numObjects, "created bucket contains 0 objects");

    // write a blob to the bucket
    String blobName = "testBlob";
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(), bucketName, blobName);

    // `terra resource describe --name=$name`
    UFGcsBucket describedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + name);

    // check that there is now 1 object reported in the bucket
    assertEquals(1, describedBucket.numObjects, "described bucket contains 1 object");
  }

  @Test
  @DisplayName("referenced bucket displays the number of objects")
  void numObjectsForReferenced() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "numObjectsForReferenced";
    UFGcsBucket addedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "add-ref",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName());

    // the external bucket created in the beforeall method should have 1 blob in it
    assertEquals(1, addedBucket.numObjects, "referenced bucket contains 1 object");
  }

  @Test
  @DisplayName("referenced bucket with no access does not fail the describe command")
  void numObjectsForReferencedWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName`
    String name = "numObjectsForReferencedWithNoAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName());

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

    // `terra resource describe --name=$name`
    UFGcsBucket describeBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + name);

    // the external bucket created in the beforeall method should have 1 blob in it, but the sharee
    // user doesn't have read access to the bucket so they can't know that
    assertNull(describeBucket.numObjects, "referenced bucket with no access contains NULL objects");
  }
}
