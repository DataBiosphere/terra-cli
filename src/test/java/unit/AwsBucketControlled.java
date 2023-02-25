package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.AwsBucketUtils;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS buckets. */
@Tag("unit-aws")
public class AwsBucketControlled extends SingleWorkspaceUnitAws {
  @Test
  @DisplayName("list and describe reflect creating a new controlled AWS bucket")
  void listDescribeReflectCreateAws() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-bucket --name=$bucketName`
    String resourceName = UUID.randomUUID().toString();
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "create", "aws-bucket", "--name=" + resourceName);

    // check that the name and bucket name match
    assertEquals(resourceName, createdBucket.name, "create output matches name");

    // check that the bucket is in the list
    UFAwsBucket matchedResource = AwsBucketUtils.listOneBucketResourceWithNameAws(resourceName);
    assertEquals(resourceName, matchedResource.name, "list output matches name");
    assertTrue(
        StringUtils.isNotBlank(matchedResource.bucketName),
        "list output has non-empty bucket name");

    // `terra resource describe --name=$name --format=json`
    UFAwsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + resourceName);

    // check that the name and bucket name match
    assertEquals(resourceName, describeResource.name, "describe resource output matches name");
    assertTrue(
        StringUtils.isNotBlank(describeResource.bucketName),
        "describe output has non-empty bucket name");

    // TODO(TERRA-148) Support bucket deletion
    // `terra resource delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + bucketName, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a controlled AWS bucket")
  void listReflectsDeleteAws() {
    // TODO(TERRA-148) Support bucket deletion
  }

  @Test
  @DisplayName("resolve a controlled AWS bucket")
  void resolveAws() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-bucket --name=$bucketName`
    String resourceName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "aws-bucket", "--name=" + resourceName);

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName);
    // String resolvedString = String.valueOf(resolved.get(resourceName));

    assertTrue(
        AwsBucketUtils.verifyS3Path(String.valueOf(resolved.get(resourceName)), resourceName, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName, "--exclude-bucket-prefix");
    assertTrue(
        AwsBucketUtils.verifyS3Path(
            String.valueOf(resolvedExcludePrefix.get(resourceName)), resourceName, false),
        "exclude prefix resolve only includes bucket name");

    // TODO(TERRA-148) Support bucket deletion
    // `terra resources delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a controlled AWS bucket")
  void checkAccessAws() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create aws-bucket --name=$bucketName`
    String resourceName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "aws-bucket", "--name=" + resourceName);

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + resourceName);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // TODO(TERRA-148) Support bucket deletion
    // `terra resources delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("create a controlled AWS bucket, specifying all options except lifecycle")
  void createWithAllOptionsExceptLifecycleAws() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create aws-bucket --name=$name --bucket-name=$bucketName --access=$access
    // --cloning=$cloning --description=$description --location=$location --storage=$storage
    // --format=json`
    String resourceName = UUID.randomUUID().toString();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
    String description = "\"create with all options except lifecycle\"";
    String location = "us-east-1";
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class,
            "resource",
            "create",
            "aws-bucket",
            "--name=" + resourceName,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location);

    // check that the properties match
    assertEquals(resourceName, createdBucket.name, "create output matches name");
    assertTrue(
        StringUtils.isNotBlank(createdBucket.bucketName),
        "create output has non-empty bucket name");
    assertEquals(access, createdBucket.accessScope, "create output matches access");
    assertEquals(cloning, createdBucket.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdBucket.description, "create output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdBucket.privateUserName.toLowerCase(),
        "create output matches private user name");

    // `terra resources describe --name=$name --format=json`
    UFAwsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + resourceName);

    // check that the properties match
    assertEquals(resourceName, describeResource.name, "describe resource output matches name");
    assertTrue(
        StringUtils.isNotBlank(describeResource.bucketName),
        "describe output has non-empty bucket name");
    assertEquals(access, describeResource.accessScope, "describe output matches access");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");

    // TODO(TERRA-148) Support bucket deletion
    // `terra resources delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + bucketName, "--quiet");
  }

  @Test
  @DisplayName("update a controlled AWS bucket, one property at a time, except for lifecycle")
  void updateIndividualPropertiesAws() {
    // TODO(TERRA-229) - support additional properties
  }

  @Test
  @DisplayName(
      "update a controlled AWS bucket, specifying multiple properties, except for lifecycle")
  void updateMultiplePropertiesAws() {
    // TODO(TERRA-229) - support additional properties
  }
}
