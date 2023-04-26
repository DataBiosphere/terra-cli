package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.AwsS3StorageFolderUtils;
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

    // `terra resource create s3-storage-folder --name=$name --folder-name=folderName`
    String folderName = UUID.randomUUID().toString();
    String name = "listDescribeResolveReflectCreateDelete" + folderName;
    UFAwsS3StorageFolder createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class,
            "resource",
            "create",
            "s3-storage-folder",
            "--name=" + name,
            "--folder-name=",
            folderName);

    // check the created workspace has an id and aws details
    assertNotNull(createdResource.bucketName, "create resource returned a aws bucket name");
    assertEquals(folderName, createdResource.prefix, "create resource prefix matches folder name");
    assertEquals(name, createdResource.name, "create resource name matches name");
    assertEquals(0, createdResource.numObjects, "create resource contains no objects");

    // check that the storage folder is in the list
    UFAwsS3StorageFolder matchedResource =
        AwsS3StorageFolderUtils.listOneStorageFolderResourceWithName(name);
    AwsS3StorageFolderUtils.assertAwsS3StorageFolderFields(
        createdResource, matchedResource, "list");

    // `terra resource describe --name=$name --format=json`
    UFAwsS3StorageFolder describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class, "resource", "describe", "--name=" + name);

    // check the new storage folder is returned by describe
    TestUtils.assertResourceProperties(createdResource, describeResource, "describe");
    AwsS3StorageFolderUtils.assertAwsS3StorageFolderFields(
        createdResource, describeResource, "describe");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertTrue(
        AwsS3StorageFolderUtils.verifyS3Path(String.valueOf(resolved.get(name)), name, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + name, "--exclude-bucket-prefix");
    assertTrue(
        AwsS3StorageFolderUtils.verifyS3Path(
            String.valueOf(resolvedExcludePrefix.get(name)), name, false),
        "exclude prefix resolve only includes storage folder name");

    // `terra resource credentials --name=$name --scope=READ_ONLY --duration=1500 --format=json`
    JSONObject resolvedCredentials =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource",
            "credentials",
            "--name=" + name,
            "--scope=" + Resource.CredentialsAccessScope.READ_ONLY,
            "--duration=" + 1500);
    assertNotNull(resolvedCredentials.get("Version"), "get credentials returned version");
    assertNotNull(resolvedCredentials.get("AccessKeyId"), "get credentials returned access key id");
    assertNotNull(
        resolvedCredentials.get("SecretAccessKey"), "get credentials returned access key");
    assertNotNull(
        resolvedCredentials.get("SessionToken"), "get credentials returned session token");
    assertNotNull(
        resolvedCredentials.get("Expiration"), "get credentials returned expiration date time");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFAwsS3StorageFolder> listedBuckets =
        AwsS3StorageFolderUtils.listStorageFolderResourcesWithName(name);
    assertThat(
        "deleted storage folder no longer appears in the resources list",
        listedBuckets,
        Matchers.empty());
  }
}
