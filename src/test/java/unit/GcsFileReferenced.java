package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGcsFile;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class GcsFileReferenced extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // name of blob in external bucket
  private String externalBucketBlobName = "blobs/testBlob";

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithUniformAccess();

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
  @DisplayName("list and describe reflect adding a new referenced bucket file")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "listDescribeReflectAdd";
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

    // check that the file is in the list
    List<UFGcsFile> matchedResourceList = listFileResourceWithName(name);
    assertEquals(1, matchedResourceList.size());
    UFGcsFile matchedResource = matchedResourceList.get(0);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalBucket.getName(), matchedResource.bucketName, "list output matches bucket name");
    assertEquals(
        externalBucketBlobName, matchedResource.filePath, "List output matches bucket file name");

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
        "describe resource output matches bucket file name");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("resolve a referenced bucket file")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "resolve";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsFile.class,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(
        ExternalGCSBuckets.getGsPath(externalBucket.getName(), externalBucketBlobName),
        resolved,
        "resolve matches bucket file name");

    // `terra resource resolve --name=$name --format=json --exclude-bucket-prefix`
    String resolveExcludeBucketPrefix =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name, "--exclude-bucket-prefix");
    assertEquals(
        externalBucket.getName() + "/" + externalBucketBlobName,
        resolveExcludeBucketPrefix,
        "resolve matches bucket file name excluding the prefix");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced file")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "listReflectsDelete";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsFile.class,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the file is not in the list
    List<UFGcsFile> matchedResources = listFileResourceWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("check-access for a referenced file")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath`
    String name = "checkAccess";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsFile.class,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // `terra resource check-access --name=$name
    TestCommand.runCommandExpectSuccess("resource", "check-access", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add a referenced file, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath --cloning=$cloning --description=$description --format=json`
    String name = "addWithAllOptionsExceptLifecycle";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "add with all options except lifecycle";
    UFGcsFile addedBucketFileReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "add-ref",
            "gcs-file",
            "--name=" + name,
            "--description=" + description,
            "--cloning=" + cloning,
            "--bucket-name=" + externalBucket.getName(),
            "--file-path=" + externalBucketBlobName);

    // check that the properties match
    assertEquals(name, addedBucketFileReference.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(),
        addedBucketFileReference.bucketName,
        "add ref output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        addedBucketFileReference.filePath,
        "add ref output matches bucket file name");
    assertEquals(
        cloning, addedBucketFileReference.cloningInstructions, "add ref output matches cloning");
    assertEquals(
        description, addedBucketFileReference.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFGcsFile describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        externalBucketBlobName,
        describeResource.filePath,
        "describe resource output matches bucket file name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a referenced file, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-file --name=$name --bucket-name=$bucketName
    // --file-path=$filePath --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsFile.class,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // update just the name
    // `terra resources update gcs-bucket --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFGcsFile updateBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "update",
            "gcs-file",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updateBucketFile.name);
    assertEquals(description, updateBucketFile.description);

    // `terra resources describe --name=$newName`
    UFGcsFile describeBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describeBucketFile.description);

    // update just the description
    // `terra resources update gcs-bucket --name=$newName --description=$newDescription`
    String newDescription = "updateDescription_NEW";
    updateBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "update",
            "gcs-file",
            "--name=" + newName,
            "--description=" + newDescription);
    assertEquals(newName, updateBucketFile.name);
    assertEquals(newDescription, updateBucketFile.description);

    // `terra resources describe --name=$newName`
    describeBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeBucketFile.description);
  }

  @Test
  @DisplayName("update a referenced file, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-file --name=$name --description=$description
    // --bucket-name=$bucketName --file-path=$filePath`
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsFile.class,
        "resource",
        "add-ref",
        "gcs-file",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalBucket.getName(),
        "--file-path=" + externalBucketBlobName);

    // call update without specifying any properties to modify
    // `terra resources update gcs-bucket --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "update", "gcs-file", "--name=" + name);
    assertThat(
        "error message says that at least one property must be specified",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update"));

    // update the name and description
    // `terra resources update gcs-file --name=$newName --new-name=$newName
    // --description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFGcsFile updateBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class,
            "resource",
            "update",
            "gcs-file",
            "--name=" + name,
            "--new-name=" + newName,
            "--description=" + newDescription);
    assertEquals(newName, updateBucketFile.name);
    assertEquals(newDescription, updateBucketFile.description);

    // `terra resources describe --name=$newName2`
    UFGcsFile describeBucketFile =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsFile.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeBucketFile.description);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFGcsFile> listFileResourceWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=GCS_FILE --format=json`
    List<UFGcsFile> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=GCS_FILE");

    // find the matching bucket in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
