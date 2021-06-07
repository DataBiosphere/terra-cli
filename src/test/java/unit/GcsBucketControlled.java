package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.inputs.GcsStorageClass;
import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.IamRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.storage.Bucket;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resources` commands that handle controlled GCS buckets. */
@Tag("unit")
public class GcsBucketControlled extends SingleWorkspaceUnit {
  @Test
  @DisplayName("list and describe reflect creating a new controlled bucket")
  void listDescribeReflectCreate() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "listDescribeReflectCreate";
    String bucketName = UUID.randomUUID().toString();
    UFGcsBucket createdBucket =
        TestCommand.runCommandExpectSuccess(
            UFGcsBucket.class,
            "resources",
            "create",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + bucketName,
            "--format=json");

    // check that the name and bucket name match
    assertEquals(name, createdBucket.name, "create output matches name");
    assertEquals(bucketName, createdBucket.bucketName, "create output matches bucket name");

    // `terra resources list --type=GCS_BUCKET --stewardship=CONTROLLED --format=json`
    List<UFGcsBucket> listedResources =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {},
            "resources",
            "list",
            "--type=GCS_BUCKET",
            "--stewardship=CONTROLLED",
            "--format=json");

    // check that the bucket is in the list
    List<UFGcsBucket> matchedResources =
        listedResources.stream()
            .filter(resource -> resource.name.equals(name))
            .collect(Collectors.toList());
    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    assertEquals(name, matchedResources.get(0).name, "list output matches name");
    assertEquals(
        bucketName, (matchedResources.get(0)).bucketName, "list output matches bucket name");

    // `terra resources describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runCommandExpectSuccess(
            UFGcsBucket.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        bucketName, describeResource.bucketName, "describe resource output matches bucket name");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("list reflects deleting a controlled bucket")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "listReflectsDelete";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + bucketName,
        "--format=json");

    // `terra resources delete --name=$name --format=json`
    UFGcsBucket deletedBucket =
        TestCommand.runCommandExpectSuccess(
            UFGcsBucket.class, "resources", "delete", "--name=" + name, "--format=json");

    // check that the name and bucket name match
    assertEquals(name, deletedBucket.name, "delete output matches name");
    assertEquals(bucketName, deletedBucket.bucketName, "delete output matches bucket name");

    // `terra resources list --type=GCS_BUCKET --stewardship=REFERENCED --format=json`
    List<UFGcsBucket> listedResources =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {},
            "resources",
            "list",
            "--type=GCS_BUCKET",
            "--stewardship=CONTROLLED",
            "--format=json");

    // check that the bucket is not in the list
    List<UFGcsBucket> matchedResources =
        listedResources.stream()
            .filter(resource -> resource.name.equals(name))
            .collect(Collectors.toList());
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a controlled bucket")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resolve";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + bucketName,
        "--format=json");

    // `terra resources resolve --name=$name --format=json`
    String resolved =
        TestCommand.runCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name, "--format=json");
    assertEquals("gs://" + bucketName, resolved, "default resolve includes gs:// prefix");

    // `terra resources resolve --name=$name --exclude-bucket-prefix --format=json`
    String resolvedExcludePrefix =
        TestCommand.runCommandExpectSuccess(
            String.class,
            "resources",
            "resolve",
            "--name=" + name,
            "--exclude-bucket-prefix",
            "--format=json");
    assertEquals(
        bucketName, resolvedExcludePrefix, "exclude prefix resolve only includes bucket name");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("check-access for a controlled bucket")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "checkAccess";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + bucketName,
        "--format=json");

    // `terra resources check-access --name=$name`
    TestCommand.Result cmd =
        TestCommand.runCommandExpectExitCode(1, "resources", "check-access", "--name=" + name);
    assertThat(
        "error message includes wrong stewardship type",
        cmd.stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("create a controlled bucket, specifying all options except lifecycle")
  void createWithAllOptionsExceptLifecycle() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --cloning=$cloning
    // --description=$description --format=json`
    String name = "createWithAllOptionsExceptLifecycle";
    String bucketName = UUID.randomUUID().toString();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "\"create with all options except lifecycle\"";
    IamRole iamRole = IamRole.WRITER;
    String location = "US";
    GcsStorageClass storage = GcsStorageClass.NEARLINE;
    UFGcsBucket createdBucket =
        TestCommand.runCommandExpectSuccess(
            UFGcsBucket.class,
            "resources",
            "create",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + bucketName,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--email=" + workspaceCreator.email.toLowerCase(),
            "--iam-roles=" + iamRole,
            "--location=" + location,
            "--storage=" + storage,
            "--format=json");

    // check that the properties match
    assertEquals(name, createdBucket.name, "create output matches name");
    assertEquals(bucketName, createdBucket.bucketName, "create output matches bucket name");
    assertEquals(access, createdBucket.accessScope, "create output matches access");
    assertEquals(cloning, createdBucket.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdBucket.description, "create output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdBucket.privateUserName.toLowerCase(),
        "create output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them
    //    assertEquals(
    //        1, createdBucket.privateUserRoles.size(), "create output matches private user roles
    // size");
    //    assertEquals(
    //        iamRole, createdBucket.privateUserRoles.get(0), "create output matches private user
    // roles");

    Bucket createdBucketOnCloud =
        ExternalGCSBuckets.getBucket(bucketName, workspaceCreator.getCredentials());
    assertNotNull(createdBucketOnCloud, "looking up bucket via GCS API succeeded");
    assertEquals(
        location, createdBucketOnCloud.getLocation(), "bucket location matches create input");
    assertEquals(
        storage.toString(),
        createdBucketOnCloud.getStorageClass().toString(),
        "bucket storage class matches create input");

    // `terra resources describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runCommandExpectSuccess(
            UFGcsBucket.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        bucketName, describeResource.bucketName, "describe resource output matches bucket name");
    assertEquals(access, describeResource.accessScope, "describe output matches access");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them
    //    assertEquals(
    //        1,
    //        describeResource.privateUserRoles.size(),
    //        "describe output matches private user roles size");
    //    assertEquals(
    //        iamRole,
    //        describeResource.privateUserRoles.get(0),
    //        "describe output matches private user roles");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }
}
