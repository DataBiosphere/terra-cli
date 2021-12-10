package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static unit.GcsObjectReferenced.listObjectResourceWithName;

import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
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
public class FineGrainedAccessGcsObjectReference extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blob in external bucket
  private String externalBucketBlobName = "foo/testBlob";
  private String externalBucketBlob2Name = "foo/";

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
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        externalBucketBlobName);
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        externalBucketBlob2Name);
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
      BlobId blobId1 = BlobId.of(externalBucket.getName(), externalBucketBlob2Name);
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
            "--object-name=" + externalBucketBlobName);

    // check that the name and bucket name match
    assertEquals(name, addedBucketObjectReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        externalBucketBlobName,
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
        externalBucketBlobName,
        describeResource.objectName,
        "describe resource output matches object name");
    assertFalse(describeResource.isDirectory);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName(
      "update reference to a bucket object that the user has access to to another bucket object that the user has access to")
  void updateObjectReferenceWithAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "updateObjectReferenceWithAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

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
        "--new-object-name=" + externalBucketBlob2Name);
    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newName, describeResource.name);
    assertEquals(newDescription, describeResource.description);
    assertEquals(externalBucket.getName(), describeResource.bucketName);
    assertEquals(externalBucketBlob2Name, describeResource.objectName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }

  @Test
  @DisplayName("user with no access try to update reference to a bucket object")
  void updateObjectReferenceWithoutAccess_failsToUpdateAnything() throws IOException {
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
        "--object-name=" + externalBucketBlobName);

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

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
        "--new-object-name=" + externalBucketBlob2Name);

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
  @DisplayName("user with partial access try to update reference to a bucket object")
  void updateObjectReferenceWithPartialAccess() throws IOException {
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
        "--object-name=" + externalBucketBlobName);

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();
    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        externalBucketBlobName,
        new Acl.Group(Auth.getProxyGroupEmail()),
        Role.READER);

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
        "--new-object-name=" + externalBucketBlob2Name);

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
    assertEquals(externalBucketBlobName, describeResource.objectName);

    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        externalBucketBlob2Name,
        new Acl.Group(Auth.getProxyGroupEmail()),
        Role.READER);
    TestCommand.runCommandExpectSuccess(
        "resource",
        "update",
        "gcs-object",
        "--name=" + newName,
        "--new-bucket-name=" + externalBucket.getName(),
        "--new-object-name=" + externalBucketBlob2Name);
    // `terra resource describe --name=$name --format=json`
    UFGcsObject describeResourceAfterUpdatingTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newName, describeResourceAfterUpdatingTarget.name);
    assertEquals(newDescription, describeResourceAfterUpdatingTarget.description);
    assertEquals(externalBucket.getName(), describeResourceAfterUpdatingTarget.bucketName);
    assertEquals(externalBucketBlob2Name, describeResourceAfterUpdatingTarget.objectName);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }

  @Test
  @DisplayName("describe the reference to a bucket object that the user has access to")
  void describeObjectReferenceWithAccess() throws IOException {
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
        "--object-name=" + externalBucketBlobName);

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
        externalBucketBlobName,
        describeResource.objectName,
        "describe resource output matches object name");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add reference to a bucket object that the user has partial access to")
  void addRefWithPartialAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();

    String proxyGroupEmail = Auth.getProxyGroupEmail();
    ExternalGCSBuckets.grantAccess(
        externalBucket.getName(),
        externalBucketBlobName,
        new Acl.Group(proxyGroupEmail),
        Role.READER);

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "addRefWithPartialAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    String noAccessBlobName = "addRefToBlobWithoutAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + noAccessBlobName,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlob2Name);

    String bucketRefName = "addRefToBucketWithoutAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + bucketRefName,
        "--bucket-name=" + externalBucket.getName());

    // check that the object is in the list
    List<UFGcsObject> matchedResourceList = listObjectResourceWithName(name);
    assertEquals(1, matchedResourceList.size());
  }

  @Test
  @DisplayName("describe reference to a bucket object that the user has READER access to")
  void describeObjectReferenceWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "describeRefWithNoAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // `terra workspace add-user --email=$email --role=READER`
    TestUsers shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();

    // `terra resource describe --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "describe", "--name=" + name);
  }
}
