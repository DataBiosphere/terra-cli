package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitAws;
import harness.utils.ExternalAwsBuckets;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests for the `terra resource` commands that handle controlled AWS buckets. */
@Tag("unit-aws")
public class AwsBucketControlled extends SingleWorkspaceUnitAws {
  private static final Logger logger = LoggerFactory.getLogger(AwsBucketControlled.class);

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  static UFAwsBucket listOneBucketResourceWithNameAws(String resourceName)
      throws JsonProcessingException {
    return listOneBucketResourceWithNameAws(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  static UFAwsBucket listOneBucketResourceWithNameAws(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsBucket> matchedResources =
        listBucketResourcesWithNameAws(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  static List<UFAwsBucket> listBucketResourcesWithNameAws(String resourceName)
      throws JsonProcessingException {
    return listBucketResourcesWithNameAws(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFAwsBucket> listBucketResourcesWithNameAws(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AWS_BUCKET --format=json`
    List<UFAwsBucket> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AWS_BUCKET")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AWS_BUCKET",
                "--workspace=" + workspaceUserFacingId);

    // find the matching bucket in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

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
    UFAwsBucket matchedResource = listOneBucketResourceWithNameAws(resourceName);
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
        ExternalAwsBuckets.verifyS3Path(
            String.valueOf(resolved.get(resourceName)), resourceName, true),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + resourceName, "--exclude-bucket-prefix");
    assertTrue(
        ExternalAwsBuckets.verifyS3Path(
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
