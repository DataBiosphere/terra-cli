package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static unit.GcsBucketControlled.listBucketResourceWithName;
import static unit.GcsBucketControlled.listOneBucketResourceWithName;

import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.google.cloud.storage.Bucket;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resources` commands that handle referenced GCS buckets. */
@Tag("unit")
public class GcsBucketReferenced extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private Bucket externalBucket;

  @BeforeAll
  @Override
  protected void setupOnce() throws IOException {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucket();
    ExternalGCSBuckets.grantReadAccess(externalBucket, workspaceCreator.email);
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws IOException {
    super.cleanupOnce();
    ExternalGCSBuckets.getStorageClient().delete(externalBucket.getName());
    externalBucket = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced bucket")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "listDescribeReflectAdd";
    UFGcsBucket addedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resources",
            "add-ref",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName());

    // check that the name and bucket name match
    assertEquals(name, addedBucket.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(), addedBucket.bucketName, "add ref output matches bucket name");

    // check that the bucket is in the list
    UFGcsBucket matchedResource = listOneBucketResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalBucket.getName(), matchedResource.bucketName, "list output matches bucket name");

    // `terra resources describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resources", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("list reflects deleting a referenced bucket")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "listReflectsDelete";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--format=json");

    // `terra resources delete --name=$name --format=json`
    UFGcsBucket deletedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resources", "delete", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, deletedBucket.name, "delete output matches name");
    assertEquals(
        externalBucket.getName(), deletedBucket.bucketName, "delete output matches bucket name");

    // check that the bucket is not in the list
    List<UFGcsBucket> matchedResources = listBucketResourceWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced bucket")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resolve";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--format=json");

    // `terra resources resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name);
    assertEquals(
        "gs://" + externalBucket.getName(), resolved, "default resolve includes gs:// prefix");

    // `terra resources resolve --name=$name --exclude-bucket-prefix --format=json`
    String resolvedExcludePrefix =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name, "--exclude-bucket-prefix");
    assertEquals(
        externalBucket.getName(),
        resolvedExcludePrefix,
        "exclude prefix resolve only includes bucket name");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("check-access for a referenced bucket")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "checkAccess";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalBucket.getName(),
        "--format=json");

    // `terra resources check-access --name=$name
    TestCommand.runCommandExpectSuccess("resources", "check-access", "--name=" + name);

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("add a referenced bucket, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --bucket-name=$bucketName --cloning=$cloning
    // --description=$description --format=json`
    String name = "addWithAllOptionsExceptLifecycle";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "add with all options except lifecycle";
    UFGcsBucket addedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resources",
            "add-ref",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + externalBucket.getName(),
            "--cloning=" + cloning,
            "--description=" + description);

    // check that the properties match
    assertEquals(name, addedBucket.name, "add ref output matches name");
    assertEquals(
        externalBucket.getName(), addedBucket.bucketName, "add ref output matches bucket name");
    assertEquals(cloning, addedBucket.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, addedBucket.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resources", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }
}
