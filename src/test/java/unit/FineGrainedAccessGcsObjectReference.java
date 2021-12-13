package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static unit.GcsObjectReferenced.listObjectResourceWithName;

import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import com.google.cloud.Identity;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Blob;
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

/** Test for CLI command on GCS objects in a fine-grained access GCS bucket. */
@Tag("unit")
public class FineGrainedAccessGcsObjectReference extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blobs in external bucket
  private String privateExternalBlobName = "foo/secret.txt";
  private String sharedExternalBlobName = "foo/";

  private TestUsers shareeUser;
  private Blob privateBlob;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithFineGrainedAccess();

    String proxyGroupEmail = Auth.getProxyGroupEmail();

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(proxyGroupEmail));

    // upload an object to the bucket
    privateBlob =
        ExternalGCSBuckets.writeBlob(
            workspaceCreator.getCredentialsWithCloudPlatformScope(),
            externalBucket.getName(),
            privateExternalBlobName);
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        sharedExternalBlobName);

    shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    shareeUser.login();
    String shareeProxyEmail = Auth.getProxyGroupEmail();

    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        sharedExternalBlobName,
        new Acl.Group(shareeProxyEmail),
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
      BlobId blobId = BlobId.of(externalBucket.getName(), privateExternalBlobName);
      storageClient.delete(blobId);
      BlobId blobId1 = BlobId.of(externalBucket.getName(), sharedExternalBlobName);
      storageClient.delete(blobId1);
    } catch (IOException ioEx) {
      System.out.println("Error deleting objects in the external bucket.");
      ioEx.printStackTrace();
    }

    ExternalGCSBuckets.deleteBucket(externalBucket);
    externalBucket = null;
  }

  @Test
  @DisplayName("add reference to a bucket object that the user has access to")
  void addObjectReferenceWithAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "addObjectReferenceWithAccess";
    UFGcsObject addedBucketObjectReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "add-ref",
            "gcs-object",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName(),
            "--object-name=" + privateExternalBlobName);

    // check that the name and bucket name match
    assertEquals(name, addedBucketObjectReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        privateExternalBlobName,
        addedBucketObjectReference.objectName,
        "add ref output matches bucket object name");

    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + name);
    
    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        privateExternalBlobName,
        describeResource.objectName,
        "describe resource output matches object name");
    assertFalse(describeResource.isDirectory);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName(
      "update reference to a bucket object that the user has access to to another bucket object that the user has bucket level access to")
  void updateObjectReferenceWithBucketLevelAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "updateObjectReferenceWithBucketLevelAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    String newName = "yetAnotherName";
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--description=" + newDescription,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + sharedExternalBlobName);
    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newName, describeResource.name);
    assertEquals(newDescription, describeResource.description);
    assertEquals(externalBucket.getName(), describeResource.bucketName);
    assertEquals(sharedExternalBlobName, describeResource.objectName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }

  @Test
  @DisplayName("user tries to update reference of a bucket object that they don't have access to")
  void attemptToUpdatePrivateBucketObject() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "updateObjectReferenceWithoutAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    String newName = "yetAnotherName";
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--description=" + newDescription,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + sharedExternalBlobName);

    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--description=" + newDescription);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectExitCode(2, "delete", "--name=" + name, "--quiet");

    workspaceCreator.login();
    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName(
      "user with partial access try to update reference to a private object - fails, update reference's name and description - succeeds")
  void userWithPartialAccessUpdateSharedBucketObject() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "updateObjectReferenceWithoutAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + sharedExternalBlobName);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    String newName = "yetAnotherName";
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--description=" + newDescription,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + privateExternalBlobName);

    TestCommand.runCommandExpectSuccess(
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--description=" + newDescription);

    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newName, describeResource.name);
    assertEquals(newDescription, describeResource.description);
    assertEquals(externalBucket.getName(), describeResource.bucketName);
    assertEquals(sharedExternalBlobName, describeResource.objectName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }

  @Test
  @DisplayName("describe the reference to a bucket object that the user has bucket level access to")
  void describeObjectReferenceWhenUserHasBucketLevelAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "describeObjectReferenceWithAccess";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        privateExternalBlobName,
        describeResource.objectName,
        "describe resource output matches object name");
    assertEquals(
        privateBlob.getSize(),
        describeResource.size,
        "describe resource output matches object size");
    assertEquals(
        privateBlob.isDirectory(),
        describeResource.isDirectory,
        "describe resource output matches object boolean isDirectory");
    assertEquals(
        privateBlob.getUpdateTime(),
        describeResource.timeStorageClassUpdated,
        "describe resource output matches object last storage update time");
    assertEquals(
        privateBlob.getContentType(),
        describeResource.contentType,
        "describe resource output matches object content type");
    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add reference to a bucket object that the user has partial access to")
  void addRefWithPartialAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace remove-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "remove-user", "--email=" + shareeUser.email, "--role=READER");
    // shareeUser needs WRITER level access to add reference resources.
    // `terra workspace add-user --email=$email --role=WRITER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String noAccessBlobName = "addRefToBucketBlobNoAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + noAccessBlobName,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    String withAccessBlobName = "addRefToBucketBlobWithAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + withAccessBlobName,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + sharedExternalBlobName);

    String bucketRefName = "addRefToBucketWithoutAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + bucketRefName,
        "--bucket-name=" + externalBucket.getName());

    // check that the object is in the list
    List<UFGcsObject> matchedResourceList = listObjectResourceWithName(withAccessBlobName);
    assertEquals(1, matchedResourceList.size());

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + withAccessBlobName, "--quiet");
  }
}
