package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import com.google.cloud.Identity;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Test for CLI command on GCS objects in a fine-grained access GCS bucket. */
@Tag("unit-gcp")
public class FineGrainedAccessGcsObjectReference extends SingleWorkspaceUnitGcp {
  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blobs in external bucket
  private String sharedExternalBlobName = "foo/text.txt";
  private String privateExternalBlobName = "foo/";

  private TestUser shareeUser;
  private Blob sharedBlob;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithFineGrainedAccess();

    String proxyGroupEmail = Auth.getProxyGroupEmail();

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(proxyGroupEmail));

    sharedBlob =
        ExternalGCSBuckets.writeBlob(
            workspaceCreator.getCredentialsWithCloudPlatformScope(),
            externalBucket.getName(),
            sharedExternalBlobName);

    shareeUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);
    shareeUser.login();

    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        sharedExternalBlobName,
        new Acl.Group(Auth.getProxyGroupEmail()),
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
  @DisplayName("add reference to a bucket object that the user has bucket-level access to")
  void addObjectReferenceWithBucketLevelAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
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

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("user with bucket level access updates object reference")
  void updateObjectReferenceWithBucketLevelAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
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

    String newName = RandomStringUtils.random(6, /*letters=*/ true, /*numbers=*/ true);
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--new-description=" + newDescription,
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
  void updatePrivateBucketObject() throws IOException {
    workspaceCreator.login();
    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(workspaceCreator);
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "attemptToUpdatePrivateBucketObject";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);

    String newName = RandomStringUtils.random(6, /*letters=*/ true, /*numbers=*/ true);
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--new-description=" + newDescription,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + sharedExternalBlobName);

    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--new-description=" + newDescription);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectExitCode(2, "resource", "delete", "--name=" + name, "--quiet");

    workspaceCreator.login();
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName(
      "user with partial access updates reference to a private object, updates reference's name and description")
  void userWithPartialAccessUpdateSharedBucketObject() throws IOException {
    workspaceCreator.login();
    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(workspaceCreator);
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "userWithPartialAccessUpdateSharedBucketObject";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + sharedExternalBlobName);

    // shareeUser needs WRITER level access to add reference resources.
    // `terra workspace add-user --email=$email --role=WRITER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);

    String newName = RandomStringUtils.random(6, /*letters=*/ true, /*numbers=*/ true);
    String newDescription = "yetAnotherDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "update",
        "gcs-object",
        "--name=" + name,
        "--new-name=" + newName,
        "--new-description=" + newDescription,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + privateExternalBlobName);

    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newName, describeResource.name);
    assertEquals(newDescription, describeResource.description);
    assertEquals(externalBucket.getName(), describeResource.bucketName);
    assertEquals(privateExternalBlobName, describeResource.objectName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("describe the reference to a bucket object that the user has no access to")
  void describeObjectReferenceWhenUserHasNoAccess() throws IOException {
    workspaceCreator.login();
    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(workspaceCreator);
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");
    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "describeObjectReferenceWhenUserHasNoAccess";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + privateExternalBlobName);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
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
    assertNull(describeResource.contentType);
    assertNull(describeResource.timeStorageClassUpdated);
    assertNull(describeResource.isDirectory);
    assertNull(describeResource.size);

    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace.id);
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("describe the reference to a bucket object that the user has bucket level access to")
  void describeObjectReferenceWhenUserHasBucketLevelAccess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
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
        "--object-name=" + sharedExternalBlobName);

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
        sharedExternalBlobName,
        describeResource.objectName,
        "describe resource output matches object name");
    assertEquals(
        sharedBlob.getSize(),
        describeResource.size,
        "describe resource output matches object size");
    assertEquals(
        sharedBlob.isDirectory(),
        describeResource.isDirectory,
        "describe resource output matches object boolean isDirectory");
    assertEquals(
        sharedBlob.getUpdateTime(),
        describeResource.timeStorageClassUpdated,
        "describe resource output matches object last storage update time");
    assertEquals(
        sharedBlob.getContentType(),
        describeResource.contentType,
        "describe resource output matches object content type");
    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }
}
