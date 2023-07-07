package unit;

import static bio.terra.cli.businessobject.Resource.CredentialsAccessScope.READ_ONLY;
import static bio.terra.cli.businessobject.Resource.Type.AWS_S3_STORAGE_FOLDER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.ResourceUtils;
import harness.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS S3 Storage Folders. */
@Tag("unit-aws")
public class AwsS3StorageFolderControlledTest extends SingleWorkspaceUnitAws {
  private static final String namePrefix = "cliTestAwsS3Folder";

  private static boolean verifyS3Path(String s3Path, String prefix, boolean includesS3Prefix) {
    return s3Path.matches(
        String.format("^%s[a-zA-Z0-9_-]+/%s/?$", (includesS3Prefix ? "[sS]3://" : ""), prefix));
  }

  private static void assertS3StorageFolderFields(
      UFAwsS3StorageFolder expected, UFAwsS3StorageFolder actual, String src) {
    assertEquals(expected.name, actual.name, "storage folder name matches that in " + src);
    assertEquals(
        expected.bucketName, actual.bucketName, "storage folder bucketName matches that in " + src);
    assertEquals(expected.prefix, actual.prefix, "storage folder prefix matches that in " + src);
    assertEquals(
        expected.numObjects, actual.numObjects, "storage folder numObjects matches that in " + src);
  }

  @Test
  @DisplayName(
      "list, describe and resolve reflect creating and deleting a controlled storage folder")
  void listDescribeResolveReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create s3-storage-folder --name=$name --folder-name=folderName`
    String storageName = TestUtils.appendRandomNumber(namePrefix);
    UFAwsS3StorageFolder createdResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class,
            "resource",
            "create",
            "s3-storage-folder",
            "--name=" + storageName,
            "--folder-name=" + storageName,
            "--region=" + AWS_REGION);

    // check the created resource has required details
    assertEquals(storageName, createdResource.name, "created resource matches name");
    assertEquals(AWS_REGION, createdResource.region, "created resource matches region");
    assertNotNull(createdResource.bucketName, "creates resource returned aws bucket name");
    assertEquals(storageName, createdResource.prefix, "created resource matches folder name");
    assertEquals(0, createdResource.numObjects, "created resource contains no objects");

    // TODO(BENCH-602): Add test for objects / verify numObjects when objects are supported

    // check that the storage folder is in the resource list
    UFAwsS3StorageFolder matchedResource =
        ResourceUtils.listOneResourceWithName(storageName, AWS_S3_STORAGE_FOLDER);
    assertS3StorageFolderFields(createdResource, matchedResource, "list");

    // `terra resource describe --name=$name`
    UFAwsS3StorageFolder describedResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsS3StorageFolder.class, "resource", "describe", "--name=" + storageName);

    // check the new storage folder is returned by describe
    TestUtils.assertResourceProperties(createdResource, describedResource, "describe");
    assertS3StorageFolderFields(createdResource, describedResource, "describe");

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + storageName);
    assertTrue(
        verifyS3Path(String.valueOf(resolved.get(storageName)), storageName, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + storageName, "--exclude-bucket-prefix");
    assertTrue(
        verifyS3Path(String.valueOf(resolvedExcludePrefix.get(storageName)), storageName, false),
        "exclude prefix resolve only includes storage folder name");

    // `terra resource credentials --name=$name --scope=READ_ONLY --duration=900 --format=json`
    JSONObject resolvedCredentials =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource",
            "credentials",
            "--name=" + storageName,
            "--scope=" + READ_ONLY,
            "--duration=" + 900);
    assertNotNull(resolvedCredentials.get("Version"), "get credentials returned version");
    assertNotNull(resolvedCredentials.get("AccessKeyId"), "get credentials returned access key id");
    assertNotNull(
        resolvedCredentials.get("SecretAccessKey"), "get credentials returned access key");
    assertNotNull(
        resolvedCredentials.get("SessionToken"), "get credentials returned session token");
    assertNotNull(
        resolvedCredentials.get("Expiration"), "get credentials returned expiration date time");

    // `terra resource open-console --name=$name --scope=READ_ONLY --duration=1500`
    TestCommand.Result result =
        TestCommand.runCommandExpectSuccess(
            "resource",
            "open-console",
            "--name=" + storageName,
            "--scope=" + Resource.CredentialsAccessScope.READ_ONLY,
            "--duration=" + 1500);
    assertThat(
        "console link is displayed",
        result.stdOut,
        containsString("Please open the following address in your browser"));
    assertThat(
        "console link is not opened in the browser",
        result.stdOut,
        not(containsString("Attempting to open that address in the default browser now...")));

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + storageName);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + storageName, "--quiet");

    // confirm it no longer appears in the resources list
    List<UFAwsS3StorageFolder> listedFolders =
        ResourceUtils.listResourcesWithName(storageName, AWS_S3_STORAGE_FOLDER);
    assertThat(
        "deleted storage folder no longer appears in the resources list",
        listedFolders,
        Matchers.empty());
  }
}
