package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static unit.GcsBucketControlled.listBucketResourcesWithName;
import static unit.GcsBucketControlled.listOneBucketResourceWithName;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.google.cloud.Identity;
import com.google.cloud.storage.BucketInfo;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle referenced GCS buckets. */
@Tag("unit")
public class GcsBucketReferenced extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalSharedBucket;
  private BucketInfo externalPrivateBucket;

  private TestUsers shareeUser;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalSharedBucket = ExternalGCSBuckets.createBucketWithUniformAccess();
    externalPrivateBucket = ExternalGCSBuckets.createBucketWithUniformAccess();

    ExternalGCSBuckets.grantWriteAccess(
        externalSharedBucket, Identity.user(workspaceCreator.email));
    ExternalGCSBuckets.grantReadAccess(
        externalPrivateBucket, Identity.user(workspaceCreator.email));

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(
        externalSharedBucket, Identity.group(Auth.getProxyGroupEmail()));
    ExternalGCSBuckets.grantReadAccess(
        externalPrivateBucket, Identity.group(Auth.getProxyGroupEmail()));

    shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    shareeUser.login();
    ExternalGCSBuckets.grantReadAccess(
        externalSharedBucket, Identity.group(Auth.getProxyGroupEmail()));
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalGCSBuckets.deleteBucket(externalSharedBucket);
    externalSharedBucket = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced bucket")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "listDescribeReflectAdd";
    UFGcsBucket addedBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "add-ref",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + externalSharedBucket.getName());

    // check that the name and bucket name match
    assertEquals(name, addedBucket.name, "add ref output matches name");
    assertEquals(
        externalSharedBucket.getName(),
        addedBucket.bucketName,
        "add ref output matches bucket name");

    // check that the bucket is in the list
    UFGcsBucket matchedResource = listOneBucketResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalSharedBucket.getName(),
        matchedResource.bucketName,
        "list output matches bucket name");

    // `terra resource describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalSharedBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(
        externalSharedBucket.getLocation(),
        describeResource.location,
        "describe resource location matches bucket location");
    assertEquals(0, describeResource.numObjects, "describe resource numObjects is zero");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced bucket")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName`
    String name = "listReflectsDelete";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalSharedBucket.getName());

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the bucket is not in the list
    List<UFGcsBucket> matchedResources = listBucketResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced bucket")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName`
    String name = "resolve";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalSharedBucket.getName());

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(
        ExternalGCSBuckets.getGsPath(externalSharedBucket.getName()),
        resolved,
        "default resolve includes gs:// prefix");

    // `terra resource resolve --name=$name --exclude-bucket-prefix --format=json`
    String resolvedExcludePrefix =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name, "--exclude-bucket-prefix");
    assertEquals(
        externalSharedBucket.getName(),
        resolvedExcludePrefix,
        "exclude prefix resolve only includes bucket name");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a referenced bucket")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName`
    String name = "checkAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalSharedBucket.getName());

    // `terra resource check-access --name=$name
    TestCommand.runCommandExpectSuccess("resource", "check-access", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
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
            "resource",
            "add-ref",
            "gcs-bucket",
            "--name=" + name,
            "--bucket-name=" + externalSharedBucket.getName(),
            "--cloning=" + cloning,
            "--description=" + description);

    // check that the properties match
    assertEquals(name, addedBucket.name, "add ref output matches name");
    assertEquals(
        externalSharedBucket.getName(),
        addedBucket.bucketName,
        "add ref output matches bucket name");
    assertEquals(cloning, addedBucket.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, addedBucket.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalSharedBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a referenced bucket, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --description=$description
    // --bucket-name=$bucketName`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalSharedBucket.getName());

    // update just the name
    // `terra resources update gcs-bucket --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFGcsBucket updateBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "update",
            "gcs-bucket",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updateBucket.name);
    assertEquals(description, updateBucket.description);

    // `terra resources describe --name=$newName`
    UFGcsBucket describeBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describeBucket.description);

    // update just the description
    // `terra resources update gcs-bucket --name=$newName --description=$newDescription`
    String newDescription = "updateDescription_NEW";
    updateBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "update",
            "gcs-bucket",
            "--name=" + newName,
            "--description=" + newDescription);
    assertEquals(newName, updateBucket.name);
    assertEquals(newDescription, updateBucket.description);

    // `terra resources describe --name=$newName`
    describeBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeBucket.description);

    updateBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "update",
            "gcs-bucket",
            "--name=" + newName,
            "--new-bucket-name=" + externalPrivateBucket.getName());
    assertEquals(externalPrivateBucket.getName(), updateBucket.bucketName);
    // `terra resources describe --name=$newName`
    describeBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + newName);
    assertEquals(externalPrivateBucket.getName(), describeBucket.bucketName);
  }

  @Test
  @DisplayName("update a referenced bucket, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref gcs-bucket --name=$name --description=$description
    // --bucket-name=$bucketName`
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--description=" + description,
        "--bucket-name=" + externalSharedBucket.getName());

    // call update without specifying any properties to modify
    // `terra resources update gcs-bucket --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "update", "gcs-bucket", "--name=" + name);
    assertThat(
        "error message says that at least one property must be specified",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update"));

    // update the name and description
    // `terra resources update gcs-bucket --name=$newName --new-name=$newName
    // --description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFGcsBucket updateBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "update",
            "gcs-bucket",
            "--name=" + name,
            "--new-name=" + newName,
            "--description=" + newDescription,
            "--new-bucket-name=" + externalPrivateBucket.getName());
    assertEquals(newName, updateBucket.name);
    assertEquals(newDescription, updateBucket.description);

    // `terra resources describe --name=$newName2`
    UFGcsBucket describeBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeBucket.description);
    assertEquals(externalPrivateBucket.getName(), describeBucket.bucketName);
  }

  @Test
  @DisplayName(
      "Attempt to update the gcs bucket while the user only have access to the externalSharedBucket")
  void updateGcsBucketWithPartialAccess() throws IOException {
    workspaceCreator.login();

    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    String name = "updateGcsBucketWithPartialAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalSharedBucket.getName());

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    String newName = "updateGcsBucketWithPartialAccess_NEW";
    UFGcsBucket updateBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "update",
            "gcs-bucket",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updateBucket.name);

    TestCommand.runCommandExpectExitCode(
        1,
        "resource",
        "update",
        "gcs-bucket",
        "--name=" + name,
        "--new-bucket-name=" + externalPrivateBucket.getName());

    // clean up
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName(
      "Attempt to descrone the gcs bucket while the user does not have access to the externalPrivateBucket")
  void describeBucketReferenceWithoutAccess() throws IOException {
    workspaceCreator.login();

    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    String name = "updateGcsBucketWithPartialAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + name,
        "--bucket-name=" + externalPrivateBucket.getName());

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    // `terra resource describe --name=$name --format=json`
    UFGcsBucket describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class, "resource", "describe", "--name=" + name);

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalPrivateBucket.getName(),
        describeResource.bucketName,
        "describe resource output matches bucket name");
    assertNull(describeResource.numObjects);
    assertNull(describeResource.location);

    // clean up
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName(
      "Attempt to add reference to buckets while the user only have access to externalSharedbucket")
  void addBucketRefWithPartialAccess() throws IOException {
    workspaceCreator.login();
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    String succeedName = "addBucketRefWithPartialAccess_withAccess";
    // `terra resource add-ref gcs-bucket --name=$name --bucket-name=$bucketName
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + succeedName,
        "--bucket-name=" + externalSharedBucket.getName());

    String failureName = "addBucketRefWithPartialAccess_withNoAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + failureName,
        "--bucket-name=" + externalPrivateBucket.getName());
  }
}
