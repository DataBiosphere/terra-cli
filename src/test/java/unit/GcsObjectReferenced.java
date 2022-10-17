package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.Identity;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Test for CLI commands for GCS objects. */
@Tag("unit")
public class GcsObjectReferenced extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;
  private BucketInfo externalBucket2;

  // name of blob in external bucket
  private String externalBucketBlobName = "blobs/testBlob";
  private String externalBucketBlobName2 = "blob2";

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFGcsObject> listObjectResourceWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=GCS_OBJECT --format=json`
    List<UFGcsObject> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=GCS_OBJECT");

    // find the matching bucket in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithUniformAccess();
    externalBucket2 = ExternalGCSBuckets.createBucketWithUniformAccess();

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(Auth.getProxyGroupEmail()));
    ExternalGCSBuckets.grantWriteAccess(externalBucket2, Identity.group(Auth.getProxyGroupEmail()));

    // upload an object to the bucket
    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        externalBucketBlobName);

    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket.getName(),
        externalBucketBlobName2);

    ExternalGCSBuckets.writeBlob(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalBucket2.getName(),
        externalBucketBlobName2);
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
      BlobId blobId1 = BlobId.of(externalBucket.getName(), externalBucketBlobName2);
      storageClient.delete(blobId1);
    } catch (IOException ioEx) {
      System.out.println("Error deleting objects in the external bucket.");
      ioEx.printStackTrace();
    }

    ExternalGCSBuckets.deleteBucket(externalBucket);
    externalBucket = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced bucket object")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "listDescribeReflectAdd";
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

    // check that the object is in the list
    List<UFGcsObject> matchedResourceList = listObjectResourceWithName(name);
    assertEquals(1, matchedResourceList.size());
    UFGcsObject matchedResource = matchedResourceList.get(0);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalBucket.getName(), matchedResource.bucketName, "list output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        matchedResource.objectName,
        "List output matches bucket object name");

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
        "describe resource output matches bucket object name");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced bucket object")
  void addRefToGcsFolder() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "addRefToGcsFolder";
    String folder = "blobs/";
    UFGcsObject addedBucketObjectReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "add-ref",
            "gcs-object",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName(),
            "--object-name=" + folder);

    // check that the name and bucket name match
    assertEquals(name, addedBucketObjectReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        folder, addedBucketObjectReference.objectName, "add ref output matches bucket object name");

    // check that the object is in the list
    List<UFGcsObject> matchedResourceList = listObjectResourceWithName(name);
    assertEquals(1, matchedResourceList.size());
    UFGcsObject matchedResource = matchedResourceList.get(0);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalBucket.getName(), matchedResource.bucketName, "list output matches bucket name");
    assertEquals(folder, matchedResource.objectName, "List output matches bucket object name");

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
        folder, describeResource.objectName, "describe resource output matches bucket object name");

    String name2 = "addReftoGcsFilesInFolder";
    String filesInFolder = "blobs/*";
    UFGcsObject addedBucketObjectReference2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "add-ref",
            "gcs-object",
            "--name=" + name2,
            "--gcs-path=gs://" + externalBucket.getName() + "/" + filesInFolder);
    // check that the name and bucket name match
    assertEquals(name2, addedBucketObjectReference2.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference2.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        filesInFolder,
        addedBucketObjectReference2.objectName,
        "add ref output matches bucket object name");

    String name3 = "addRefToGcsTextFilesInFolder";
    String textFilesInFolder = "blobs/*.txt";
    UFGcsObject addedBucketObjectReference3 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "add-ref",
            "gcs-object",
            "--name=" + name3,
            "--bucket-name=" + externalBucket.getName(),
            "--object-name=" + textFilesInFolder);

    // call add without gcs path
    // `terra resources add-ref gcs-bucket --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "add-ref", "gcs-object", "--name=" + name);
    assertThat(
        "Specify at least one path to update.",
        stdErr,
        CoreMatchers.containsString("Specify at least one path to update."));

    // check that the name and bucket name match
    assertEquals(name3, addedBucketObjectReference3.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference3.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        textFilesInFolder,
        addedBucketObjectReference3.objectName,
        "add ref output matches bucket object name");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name2, "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name3, "--quiet");
  }

  @Test
  @DisplayName("resolve a referenced bucket object")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "resolve";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertEquals(
        ExternalGCSBuckets.getGsPath(externalBucket.getName(), externalBucketBlobName),
        resolved.get(name),
        "resolve matches bucket object name");

    // `terra resource resolve --name=$name --format=json --exclude-bucket-prefix`
    JSONObject resolveExcludeBucketPrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + name, "--exclude-bucket-prefix");
    assertEquals(
        externalBucket.getName() + "/" + externalBucketBlobName,
        resolveExcludeBucketPrefix.get(name),
        "resolve matches bucket object name excluding the prefix");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced object")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "listReflectsDelete";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the object is not in the list
    List<UFGcsObject> matchedResources = listObjectResourceWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("check-access for a referenced object")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName`
    String name = "checkAccess";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // `terra resource check-access --name=$name
    TestCommand.runCommandExpectSuccess("resource", "check-access", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add a referenced object, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources add-ref gcs-object --name=$name --description=$description
    // --cloning=$cloning --gcs-path=$bucketName+$objectName --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "add with all options";
    UFGcsObject addedBucketObjectReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "add-ref",
            "gcs-object",
            "--name=" + name,
            "--description=" + description,
            "--cloning=" + cloning,
            "--gcs-path=gs://" + externalBucket.getName() + "/" + externalBucketBlobName);

    // check that the properties match
    assertEquals(name, addedBucketObjectReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketObjectReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        addedBucketObjectReference.objectName,
        "add ref output matches bucket object name");
    assertEquals(
        cloning, addedBucketObjectReference.cloningInstructions, "add ref output matches cloning");
    assertEquals(
        description, addedBucketObjectReference.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFGcsObject describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        describeResource.objectName,
        "describe resource output matches bucket object name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a referenced object, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref gcs-object --name=$name --bucket-name=$bucketName
    // --object-name=$objectName --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // update just the name
    // `terra resources update gcs-bucket --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFGcsObject updatedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updatedBucketObject.name);
    assertEquals(description, updatedBucketObject.description);

    // `terra resources describe --name=$newName`
    UFGcsObject describedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describedBucketObject.description);
    assertEquals(CloningInstructionsEnum.REFERENCE, describedBucketObject.cloningInstructions);

    // update description and cloning instructions
    // `terra resources update gcs-bucket --name=$newName --new-description=$newDescription
    // --new-cloning=$CloningInstructionsEnum.NOTHING`
    String newDescription = "updateDescription_NEW";
    updatedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + newName,
            "--new-description=" + newDescription,
            "--new-cloning=" + CloningInstructionsEnum.NOTHING);
    assertEquals(newName, updatedBucketObject.name);
    assertEquals(newDescription, updatedBucketObject.description);
    // assertEquals(CloningInstructionsEnum.NOTHING, updatedBucketObject.cloningInstructions);

    // `terra resources describe --name=$newName`
    describedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describedBucketObject.description);
    assertEquals(CloningInstructionsEnum.NOTHING, describedBucketObject.cloningInstructions);

    updatedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + newName,
            "--new-object-name=" + externalBucketBlobName2);
    assertEquals(externalBucketBlobName2, updatedBucketObject.objectName);
    assertEquals(externalBucket.getName(), updatedBucketObject.bucketName);

    var resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + newName);
    assertEquals(
        ExternalGCSBuckets.getGsPath(externalBucket.getName(), externalBucketBlobName2),
        resolved.get(newName),
        "resolve matches bucket object blob2 name");

    updatedBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + newName,
            "--new-bucket-name=" + externalBucket2.getName());
    assertEquals(externalBucketBlobName2, updatedBucketObject.objectName);
    assertEquals(externalBucket2.getName(), updatedBucketObject.bucketName);

    var resolved2 =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + newName);
    assertEquals(
        ExternalGCSBuckets.getGsPath(externalBucket2.getName(), externalBucketBlobName2),
        resolved2.get(newName),
        "resolve matches bucket2 bucket name");
  }

  @Test
  @DisplayName("update a referenced object, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources add-ref gcs-object --name=$name --description=$description
    // --bucket-name=$bucketName --object-name=$objectName`
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsObject.class,
        "resource",
        "add-ref",
        "gcs-object",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalBucket.getName(),
        "--object-name=" + externalBucketBlobName);

    // call update without specifying any properties to modify
    // `terra resources update gcs-bucket --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "update", "gcs-object", "--name=" + name);
    assertThat(
        "Specify at least one property to update..",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update."));

    // call update by specify multiple gcs path
    // `terra resources update gcs-object --name=$name --new-bucket-name=$newBucketName
    // --new-gcs-path=gs://$newBucketName+$newObjectName`
    String stdErr2 =
        TestCommand.runCommandExpectExitCode(
            1,
            "resource",
            "update",
            "gcs-object",
            "--name=" + name,
            "--new-bucket-name=" + externalBucket.getName(),
            "--new-gcs-path=gs://" + externalBucket.getName() + "/" + externalBucketBlobName);
    assertThat(
        "Specify only one path to add reference.",
        stdErr2,
        CoreMatchers.containsString("Specify only one path to add reference."));

    // update the name and description
    // `terra resources update gcs-object --name=$name --new-name=$newName
    // --new-description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFGcsObject updateBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription);
    assertEquals(newName, updateBucketObject.name);
    assertEquals(newDescription, updateBucketObject.description);

    // `terra resources describe --name=$newName2`
    UFGcsObject describeBucketObject =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeBucketObject.description);

    // update referencing target
    // `terra resources update gcs-object --name=$name --new-bucket-name=$newBucketName
    // --new-name=$newName --new-description=$newDescription --new-object-name=$newObjectName`
    String yetAnotherName = "updateMultipleOrNoProperties_NEW";
    String yetAnotherDescription = "updateDescription_NEW";
    UFGcsObject updateBucketObjectReferencingTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + newName,
            "--new-name=" + yetAnotherName,
            "--new-description=" + yetAnotherDescription,
            "--new-bucket-name=" + externalBucket.getName(),
            "--new-object-name=" + externalBucketBlobName2);
    assertEquals(externalBucket.getName(), updateBucketObjectReferencingTarget.bucketName);
    assertEquals(externalBucketBlobName2, updateBucketObjectReferencingTarget.objectName);

    UFGcsObject describeBucketObjectUpdatingReferencingTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class, "resource", "describe", "--name=" + yetAnotherName);
    assertEquals(yetAnotherDescription, describeBucketObjectUpdatingReferencingTarget.description);
    assertEquals(externalBucketBlobName2, describeBucketObjectUpdatingReferencingTarget.objectName);
    assertEquals(
        externalBucket.getName(), describeBucketObjectUpdatingReferencingTarget.bucketName);
    assertEquals(yetAnotherName, describeBucketObjectUpdatingReferencingTarget.name);

    // update referencing target
    // `terra resources update gcs-object --name=$name --new-gcs-path=$newBucketName+$newObjectName`
    UFGcsObject updateObjectPathReferencingTarget =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsObject.class,
            "resource",
            "update",
            "gcs-object",
            "--name=" + yetAnotherName,
            "--new-gcs-path=gs://" + externalBucket2.getName() + "/" + externalBucketBlobName);
    assertEquals(externalBucket2.getName(), updateObjectPathReferencingTarget.bucketName);
    assertEquals(externalBucketBlobName, updateObjectPathReferencingTarget.objectName);
  }
}
