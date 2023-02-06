package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AWS buckets. */
@Tag("unit-aws")
public class AwsBucketControlled extends SingleWorkspaceUnitAws {
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
  static UFAwsBucket listOneBucketResourceWithNameAws(String resourceName, String userFacingId)
      throws JsonProcessingException {
    List<UFAwsBucket> matchedResources = listBucketResourcesWithNameAws(resourceName, userFacingId);

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
    String bucketName = UUID.randomUUID().toString();
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "create", "aws-bucket", "--name=" + bucketName);

    // check that the name and bucket name match
    assertEquals(bucketName, createdBucket.name, "create output matches name");

    // check that the bucket is in the list
    UFAwsBucket matchedResource = listOneBucketResourceWithNameAws(bucketName);
    assertEquals(bucketName, matchedResource.name, "list output matches name");
    assertEquals(bucketName, matchedResource.bucketName, "list output matches bucket name");

    // `terra resource describe --name=$name --format=json`
    UFAwsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + bucketName);

    // check that the name and bucket name match
    assertEquals(bucketName, describeResource.name, "describe resource output matches name");
    assertEquals(
        bucketName, describeResource.bucketName, "describe resource output matches bucket name");

    // TODO(TERRA-148) Support bucket deletion
    // `terra resource delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + bucketName, "--quiet");
  }

  @Test
  @DisplayName("create a new controlled AWS bucket without specifying the bucket name")
  void createBucketWithoutSpecifyingBucketNameAws() throws IOException {
    // TODO-dex
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create aws-bucket --name=$bucketName`
    String bucketName = UUID.randomUUID().toString();
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "create", "aws-bucket", "--name=" + bucketName);

    // check that the name and bucket name match
    assertEquals(bucketName, createdBucket.name, "create output matches name");

    // check that the bucket is in the list
    UFAwsBucket matchedResource = listOneBucketResourceWithNameAws(bucketName);
    assertEquals(bucketName, matchedResource.name, "list output matches name");
    assertEquals(bucketName, matchedResource.bucketName, "list output matches bucket name");

    // `terra resource describe --name=$name --format=json`
    UFAwsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + bucketName);

    // check that the name and bucket name match
    assertEquals(bucketName, describeResource.name, "describe resource output matches name");
    assertEquals(
        bucketName, describeResource.bucketName, "describe resource output matches bucket name");

    // TODO(TERRA-363)
    // `terra resource delete --name=$name`
    // TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
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
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess("resource", "create", "aws-bucket", "--name=" + bucketName);

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + bucketName);
    assertEquals(
        ExternalAwsBuckets.getS3Path("", bucketName),
        resolved.get(bucketName),
        "default resolve includes s3:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    JSONObject resolvedExcludePrefix =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + bucketName, "--exclude-bucket-prefix");
    assertEquals(
        bucketName,
        resolvedExcludePrefix.get(bucketName),
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
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess("resource", "create", "aws-bucket", "--name=" + bucketName);

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + bucketName);
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
    String bucketName = UUID.randomUUID().toString();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
    String description = "\"create with all options except lifecycle\"";
    String location = "US";
    UFAwsBucket createdBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class,
            "resource",
            "create",
            "aws-bucket",
            "--name=" + bucketName,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location);

    // check that the properties match
    assertEquals(bucketName, createdBucket.name, "create output matches name");
    assertEquals(bucketName, createdBucket.bucketName, "create output matches bucket name");
    assertEquals(access, createdBucket.accessScope, "create output matches access");
    assertEquals(cloning, createdBucket.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdBucket.description, "create output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdBucket.privateUserName.toLowerCase(),
        "create output matches private user name");

    /*
       Bucket createdBucketOnCloud =
           CrlUtils.callGcpWithPermissionExceptionRetries(
               () ->
                   ExternalGCSBuckets.getStorageClient(
                           workspaceCreator.getCredentialsWithCloudPlatformScope())
                       .get(bucketName));
       assertNotNull(createdBucketOnCloud, "looking up bucket via AWS API succeeded");
       assertEquals(
           location, createdBucketOnCloud.getLocation(), "bucket location matches create input");

    */

    // `terra resources describe --name=$name --format=json`
    UFAwsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAwsBucket.class, "resource", "describe", "--name=" + bucketName);

    // check that the properties match
    assertEquals(bucketName, describeResource.name, "describe resource output matches name");
    assertEquals(
        bucketName, describeResource.bucketName, "describe resource output matches bucket name");
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
