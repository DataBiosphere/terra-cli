package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.resource.UFAwsStorageFolder;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.AwsStorageFolderUtils;
import harness.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS storage folders. */
@Tag("unit-aws")
public class AwsControlledStorageFolder extends SingleWorkspaceUnitAws {

  @Test
  @DisplayName(
      "list, describe and resolve reflect creating and deleting a controlled storage folder")
  void listDescribeResolveReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-storage-folder --name=$storageFolderName`
    String resourceName = UUID.randomUUID().toString();
    UFAwsStorageFolder createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsStorageFolder.class,
            "resource",
            "create",
            "aws-storage-folder",
            "--name=" + resourceName);

    // check the created workspace has an id and aws details
    assertNotNull(createdResource.id, "create resource returned a resource id");
    assertNotNull(createdResource.bucketName, "create resource returned a aws bucket name");
    assertNotNull(createdResource.prefix, "create resource returned a aws prefix");
    assertNotNull(createdResource.region, "create resource returned a aws region");
    assertEquals(resourceName, createdResource.name, "create resource resource name matches name");
    assertEquals(
        createdResource.resourceType,
        Resource.Type.AWS_STORAGE_FOLDER,
        "create resource resource type matches AWS_STORAGE_FOLDER");
    assertEquals(createdResource.numObjects, 0, "create resource contains no objects");

    // check that the storage folder is in the list
    UFAwsStorageFolder matchedResource =
        AwsStorageFolderUtils.listOneStorageFolderResourceWithName(resourceName);
    AwsStorageFolderUtils.assertAwsStorageFolderFields(createdResource, matchedResource, "list");

    // `terra resource describe --name=$name --format=json`
    UFAwsStorageFolder describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsStorageFolder.class, "resource", "describe", "--name=" + resourceName);

    // check the new storage folder is returned by describe
    AwsStorageFolderUtils.assertAwsStorageFolderFields(
        createdResource, describeResource, "describe");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName);
    assertTrue(
        AwsStorageFolderUtils.verifyS3Path(
            String.valueOf(resolved.get(resourceName)), resourceName, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName, "--exclude-bucket-prefix");
    assertTrue(
        AwsStorageFolderUtils.verifyS3Path(
            String.valueOf(resolvedExcludePrefix.get(resourceName)), resourceName, false),
        "exclude prefix resolve only includes storage folder name");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + resourceName);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resourceName, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFAwsStorageFolder> listedBuckets =
        AwsStorageFolderUtils.listStorageFolderResourcesWithName(resourceName);
    assertThat(
        "deleted storage folder no longer appears in the resources list",
        listedBuckets,
        Matchers.empty());
  }

  @Test
  @DisplayName("create a controlled storage folder, specifying all options except lifecycle")
  void createWithAllOptionsExceptLifecycle() throws IOException {
    // TODO(TERRA-221) - support additional properties
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create aws-storage-folder --name=$name --access=$access
    // --cloning=$cloning --description=$description --location=$location --format=json`
    String resourceName = UUID.randomUUID().toString();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
    String description = "\"create with all options except lifecycle\"";
    String location = "us-east-1";
    UFAwsStorageFolder createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsStorageFolder.class,
            "resource",
            "create",
            "aws-storage-folder",
            "--name=" + resourceName,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location);

    // check that the properties match
    assertNotNull(createdResource.id, "create resource returned a resource id");

    assertEquals(resourceName, createdResource.name, "create resource output matches name");
    assertEquals(access, createdResource.accessScope, "create resource output matches access");
    assertEquals(
        cloning, createdResource.cloningInstructions, "create resource output matches cloning");
    assertEquals(
        description, createdResource.description, "create resource output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdResource.privateUserName.toLowerCase(),
        "create resource output matches private user name");

    // `terra resources describe --name=$name --format=json`
    UFAwsStorageFolder describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsStorageFolder.class, "resource", "describe", "--name=" + resourceName);

    // check that the properties match
    TestUtils.assertResourceProperties(createdResource, describeResource, "describe");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resourceName, "--quiet");
  }
}
