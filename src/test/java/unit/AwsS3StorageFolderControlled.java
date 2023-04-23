package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.AwsS3StorageFolderUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS S3 Storage Folders. */
@Tag("unit-aws")
public class AwsS3StorageFolderControlled extends SingleWorkspaceUnitAws {

  @Test
  @DisplayName(
      "list, describe and resolve reflect creating and deleting a controlled storage folder")
  void listDescribeResolveReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create s3-storage-folder --name=$storageFolderName`
    String resourceName = UUID.randomUUID().toString();
    UFAwsS3StorageFolder createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class,
            "resource",
            "create",
            "s3-storage-folder",
            "--name=" + resourceName);

    // check the created workspace has an id and aws details
    assertNotNull(createdResource.bucketName, "create resource returned a aws bucket name");
    assertNotNull(createdResource.prefix, "create resource returned a aws prefix");
    assertEquals(resourceName, createdResource.name, "create resource resource name matches name");
    assertEquals(createdResource.numObjects, 0, "create resource contains no objects");

    // check that the storage folder is in the list
    UFAwsS3StorageFolder matchedResource =
        AwsS3StorageFolderUtils.listOneStorageFolderResourceWithName(resourceName);
    AwsS3StorageFolderUtils.assertAwsS3StorageFolderFields(
        createdResource, matchedResource, "list");

    // `terra resource describe --name=$name --format=json`
    UFAwsS3StorageFolder describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class, "resource", "describe", "--name=" + resourceName);

    // check the new storage folder is returned by describe
    AwsS3StorageFolderUtils.assertAwsS3StorageFolderFields(
        createdResource, describeResource, "describe");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName);
    assertTrue(
        AwsS3StorageFolderUtils.verifyS3Path(
            String.valueOf(resolved.get(resourceName)), resourceName, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName, "--exclude-bucket-prefix");
    assertTrue(
        AwsS3StorageFolderUtils.verifyS3Path(
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
    List<UFAwsS3StorageFolder> listedBuckets =
        AwsS3StorageFolderUtils.listStorageFolderResourcesWithName(resourceName);
    assertThat(
        "deleted storage folder no longer appears in the resources list",
        listedBuckets,
        Matchers.empty());
  }
}
