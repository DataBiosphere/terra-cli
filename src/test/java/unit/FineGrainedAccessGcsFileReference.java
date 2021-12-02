package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static unit.GcsFileReferenced.listFileResourceWithName;

import bio.terra.cli.serialization.userfacing.resource.UFGcsFile;
import com.google.cloud.Identity;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FineGrainedAccessGcsFileReference extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blob in external bucket
  private String externalBucketBlobName = "testBlob";

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithFineGrainedAccess();

    String proxyGroupEmail = Auth.getProxyGroupEmail();

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(proxyGroupEmail));

    // upload a file to the bucket
    ExternalGCSBuckets.writeBlob(
            workspaceCreator.getCredentialsWithCloudPlatformScope(),
            externalBucket.getName(),
            externalBucketBlobName);

    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        externalBucketBlobName,
        new Acl.Group(proxyGroupEmail),
        Role.READER);
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
  @DisplayName("add reference to a bucket file that the user has access to")
  void addFileReferenceWithAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "addFileReferenceWithAccess";
    UFGcsFile addedBucketFileReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "add-ref",
            "gcs-file",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName(),
            "--file-path=" + externalBucketBlobName);

    // check that the name and bucket name match
    assertEquals(name, addedBucketFileReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketFileReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        addedBucketFileReference.filePath,
        "add ref output matches bucket file name");

    // `terra resource describe --name=$name --format=json`
    UFGcsFile describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        describeResource.filePath,
        "describe resource output matches file name");
    assertFalse(describeResource.isDirectory);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("describe the reference to a bucket file that the user has access to")
  void describeFileReferenceWithAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "describeFileReferenceWithAccess";
    UFGcsFile addedBucketFileReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "add-ref",
            "gcs-file",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName(),
            "--file-path=" + externalBucketBlobName);

    // `terra resource describe --name=$name --format=json`
    UFGcsFile describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        describeResource.filePath,
        "describe resource output matches file name");
    assertFalse(describeResource.isDirectory);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add reference to a bucket file that the user has no access to")
  void addRefWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "addRefWithNoAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // check that the file is in the list
    List<UFGcsFile> matchedResourceList = listFileResourceWithName(name);
    assertEquals(0, matchedResourceList.size());
  }

  @Test
  @DisplayName("describe reference to a bucket file that the user has no access to")
  void describeFileReferenceWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "addRefWithNoAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // `terra resource describe --name=$name --format=json`
    TestCommand.runCommandExpectExitCode(1, "resource", "describe", "--name=" + name);
  }
}
