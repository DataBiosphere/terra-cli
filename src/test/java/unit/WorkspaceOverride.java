package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.AiNotebookControlled.listNotebookResourcesWithName;
import static unit.AiNotebookControlled.listOneNotebookResourceWithName;
import static unit.AiNotebookControlled.pollDescribeForNotebookState;
import static unit.BqDatasetControlled.listDatasetResourcesWithName;
import static unit.BqDatasetControlled.listOneDatasetResourceWithName;
import static unit.GcsBucketControlled.listBucketResourcesWithName;
import static unit.GcsBucketControlled.listOneBucketResourceWithName;
import static unit.Workspace.listWorkspacesWithId;
import static unit.WorkspaceUser.expectListedUserWithRoles;
import static unit.WorkspaceUser.workspaceListUsersWithEmail;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceUser;
import bio.terra.cli.serialization.userfacing.resources.UFAiNotebook;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.workspace.model.IamRole;
import com.google.cloud.Identity;
import com.google.cloud.bigquery.Acl;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.storage.Bucket;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import harness.utils.ExternalGCSBuckets;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `--workspace` option to override the current workspace just for this command. */
@Tag("unit")
public class WorkspaceOverride extends ClearContextUnit {
  protected static final TestUsers workspaceCreator = TestUsers.chooseTestUserWithSpendAccess();
  private static UFWorkspace workspace1;
  private static UFWorkspace workspace2;

  // external bucket to use for creating GCS bucket references in the workspace
  private Bucket externalBucket;

  // external dataset to use for creating BQ dataset references in the workspace
  private Dataset externalDataset;

  /**
   * Create two workspaces for tests to use, so we can switch between them with the override option.
   */
  @BeforeAll
  protected void setupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    workspaceCreator.login();

    // grant the user's proxy group access to the bucket and dataset so that they will pass WSM's
    // access check when adding them as referenced resources
    externalBucket = ExternalGCSBuckets.createBucket();
    ExternalGCSBuckets.grantReadAccess(externalBucket, Identity.user(workspaceCreator.email));
    ExternalGCSBuckets.grantReadAccess(externalBucket, Identity.group(Auth.getProxyGroupEmail()));
    externalDataset = ExternalBQDatasets.createDataset();
    ExternalBQDatasets.grantReadAccess(externalDataset, new Acl.User(workspaceCreator.email));
    ExternalBQDatasets.grantReadAccess(externalDataset, new Acl.Group(Auth.getProxyGroupEmail()));

    // `terra workspace create --format=json`
    workspace1 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // `terra workspace create --format=json`
    workspace2 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
  }

  /** Delete the two workspaces. */
  @AfterAll
  protected void cleanupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete");

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace2.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete");

    ExternalGCSBuckets.getStorageClient().delete(externalBucket.getName());
    externalBucket = null;
    ExternalBQDatasets.getBQClient().delete(externalDataset.getDatasetId());
    externalDataset = null;
  }

  @Test
  @DisplayName("gcloud and app execute respect workspace override")
  void gcloudAppExecute() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra app execute --workspace=$id2 echo \$GOOGLE_CLOUD_PROJECT`
    TestCommand.Result cmd =
        TestCommand.runCommand(
            "app", "execute", "--workspace=" + workspace2.id, "echo", "$GOOGLE_CLOUD_PROJECT");

    // check that the google cloud project id matches workspace 2
    assertThat(
        "GOOGLE_CLOUD_PROJECT set to workspace2's project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace2.googleProjectId));

    // `terra gcloud --workspace=$id2 config get-value project`
    cmd =
        TestCommand.runCommand(
            "gcloud", "--workspace=" + workspace2.id, "config", "get-value", "project");

    // check that the google cloud project id matches workspace 2
    assertThat(
        "gcloud project = workspace2's project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace2.googleProjectId));
  }

  @Test
  @DisplayName("referenced resource commands respect workspace override")
  void referencedResources() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resources add-ref gcs-bucket --name=$resourceNameBucket --bucket-name=$bucketName
    // --workspace=$id2`
    String resourceNameBucket = "referencedResources_bucket";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "gcs-bucket",
        "--name=" + resourceNameBucket,
        "--bucket-name=" + externalBucket.getName(),
        "--workspace=" + workspace2.id);

    // `terra resources add-ref bq-dataset --name=$resourceNameDataset --dataset-id=$datasetId
    // --workspace=$id2`
    String resourceNameDataset = "referencedResources_dataset";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "bq-dataset",
        "--name=" + resourceNameDataset,
        "--project-id=" + externalDataset.getDatasetId().getProject(),
        "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
        "--workspace=" + workspace2.id);

    // check that workspace 2 contains both referenced resources above and workspace 1 does not

    // `terra resources list --type=GCS_BUCKET --workspace=$id2`
    UFGcsBucket matchedBucket = listOneBucketResourceWithName(resourceNameBucket, workspace2.id);
    assertEquals(
        resourceNameBucket, matchedBucket.name, "list output for workspace 2 matches bucket name");

    // `terra resources list --type=GCS_BUCKET`
    List<UFGcsBucket> matchedBuckets = listBucketResourcesWithName(resourceNameBucket);
    assertEquals(0, matchedBuckets.size(), "list output for bucket in workspace 1 is empty");

    // `terra resources list --type=BQ_DATASET --workspace=$id2`
    UFBqDataset matchedDataset = listOneDatasetResourceWithName(resourceNameDataset, workspace2.id);
    assertEquals(
        resourceNameDataset,
        matchedDataset.name,
        "list output for workspace 2 matches dataset name");

    // `terra resources list --type=BQ_DATASET`
    List<UFBqDataset> matchedDatasets = listDatasetResourcesWithName(resourceNameDataset);
    assertEquals(0, matchedDatasets.size(), "list output for dataset in workspace 1 is empty");

    // check that check-access, describe, and resolve succeed in workspace 2 but not in workspace 1

    // `terra check-access --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resources",
        "check-access",
        "--name=" + resourceNameBucket,
        "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resources", "check-access", "--name=" + resourceNameBucket);

    // `terra describe --name=$resourceNameDataset`
    TestCommand.runCommandExpectSuccess(
        "resources", "describe", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resources", "describe", "--name=" + resourceNameDataset);

    // `terra resolve --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resources", "resolve", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(1, "resources", "resolve", "--name=" + resourceNameBucket);

    // `terra delete --name=$resourceNameBucket --workspace=$id2`
    TestCommand.runCommandExpectExitCode(1, "resources", "delete", "--name=" + resourceNameBucket);
    TestCommand.runCommandExpectSuccess(
        "resources", "delete", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);

    // `terra delete --name=$resourceNameDataset --workspace=$id2`
    TestCommand.runCommandExpectExitCode(1, "resources", "delete", "--name=" + resourceNameDataset);
    TestCommand.runCommandExpectSuccess(
        "resources", "delete", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
  }

  @Test
  @DisplayName("controlled resource commands respect workspace override")
  void controlledResources() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resources create gcs-bucket --name=$resourceNameBucket --bucket-name=$bucketName
    // --workspace=$id2`
    String resourceNameBucket = "controlledResources_bucket";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "gcs-bucket",
        "--name=" + resourceNameBucket,
        "--bucket-name=" + bucketName,
        "--workspace=" + workspace2.id);

    // `terra resources create bq-dataset --name=$resourceNameDataset --dataset-id=$datasetId
    // --workspace=$id2`
    String resourceNameDataset = "controlledResources_dataset";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "bq-dataset",
        "--name=" + resourceNameDataset,
        "--dataset-id=" + datasetId,
        "--workspace=" + workspace2.id);

    // check that workspace 2 contains both controlled resources above and workspace 1 does not

    // `terra resources list --type=GCS_BUCKET --workspace=$id2`
    UFGcsBucket matchedBucket = listOneBucketResourceWithName(resourceNameBucket, workspace2.id);
    assertEquals(
        resourceNameBucket, matchedBucket.name, "list output for workspace 2 matches bucket name");

    // `terra resources list --type=GCS_BUCKET`
    List<UFGcsBucket> matchedBuckets = listBucketResourcesWithName(resourceNameBucket);
    assertEquals(0, matchedBuckets.size(), "list output for bucket in workspace 1 is empty");

    // `terra resources list --type=BQ_DATASET --workspace=$id2`
    UFBqDataset matchedDataset = listOneDatasetResourceWithName(resourceNameDataset, workspace2.id);
    assertEquals(
        resourceNameDataset,
        matchedDataset.name,
        "list output for workspace 2 matches dataset name");

    // `terra resources list --type=BQ_DATASET`
    List<UFBqDataset> matchedDatasets = listDatasetResourcesWithName(resourceNameDataset);
    assertEquals(0, matchedDatasets.size(), "list output for dataset in workspace 1 is empty");

    // check that describe and resolve succeed in workspace 2 but not in workspace 1

    // `terra describe --name=$resourceNameDataset`
    TestCommand.runCommandExpectSuccess(
        "resources", "describe", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resources", "describe", "--name=" + resourceNameDataset);

    // `terra resolve --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resources", "resolve", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(1, "resources", "resolve", "--name=" + resourceNameBucket);

    // `terra delete --name=$resourceNameBucket --workspace=$id2`
    TestCommand.runCommandExpectExitCode(1, "resources", "delete", "--name=" + resourceNameBucket);
    TestCommand.runCommandExpectSuccess(
        "resources", "delete", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);

    // `terra delete --name=$resourceNameDataset --workspace=$id2`
    TestCommand.runCommandExpectExitCode(1, "resources", "delete", "--name=" + resourceNameDataset);
    TestCommand.runCommandExpectSuccess(
        "resources", "delete", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
  }

  @Test
  @DisplayName("workspace user commands respect workspace override")
  void workspaceUser() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUsers testUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace add-user --email=$email --role=READER --workspace=$id2`
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "add-user",
        "--email=" + testUser.email,
        "--role=READER",
        "--workspace=" + workspace2.id);

    // check that the user exists in the list output for workspace 2, but not workspace 1

    // `terra workspace list-users --email=$email --role=READER --workspace=$id2`
    expectListedUserWithRoles(testUser.email, workspace2.id, IamRole.READER);

    // `terra workspace list-users --email=$email --role=READER`
    Optional<UFWorkspaceUser> workspaceUser = workspaceListUsersWithEmail(testUser.email);
    assertTrue(workspaceUser.isEmpty(), "test user is not in users list for workspace 1");

    // `terra workspace remove-user --email=$email --role=READER --workspace=$id2`
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "remove-user",
        "--email=" + testUser.email,
        "--role=READER",
        "--workspace=" + workspace2.id);

    // `terra workspace list-users --email=$email --role=READER --workspace=$id2`
    // check that the user no longer exists in the list output for workspace 2
    workspaceUser = workspaceListUsersWithEmail(testUser.email, workspace2.id);
    assertTrue(workspaceUser.isEmpty(), "test user is no longer in users list for workspace 2");
  }

  @Test
  @DisplayName("workspace commands respect workspace override")
  void workspace() throws IOException {
    workspaceCreator.login();

    // `terra workspace create`
    UFWorkspace workspace3 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace update --name=$newName --description=$newDescription --workspace=$id3`
    String newName = "workspace3_name_NEW";
    String newDescription = "workspace3 description NEW";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--name=" + newName,
        "--description=" + newDescription,
        "--workspace=" + workspace3.id);

    // check that the workspace 3 description has been updated, and the workspace 1 has not

    // `terra workspace describe --workspace=$id3`
    UFWorkspace workspaceDescribe =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "describe", "--workspace=" + workspace3.id);
    assertEquals(newName, workspaceDescribe.name, "workspace 3 name matches update");
    assertEquals(
        newDescription, workspaceDescribe.description, "workspace 3 description matches update");

    // `terra workspace describe`
    workspaceDescribe =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(workspace1.name, workspaceDescribe.name, "workspace 1 name matches create");
    assertEquals(
        workspace1.description,
        workspaceDescribe.description,
        "workspace 1 description matches create");

    // check the workspace 3 has been deleted, and workspace 1 has not

    // `terra workspace delete --workspace=$id3`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--workspace=" + workspace3.id);

    // `terra workspace list`
    List<UFWorkspace> matchingWorkspaces = listWorkspacesWithId(workspace3.id);
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace 3 is not included in list");

    // `terra workspace list`
    matchingWorkspaces = listWorkspacesWithId(workspace1.id);
    assertEquals(1, matchingWorkspaces.size(), "workspace 1 is still included in list");
  }

  @Disabled // TODO (PF-869): enable this test once polling notebook operations is fixed
  @DisplayName("notebook commands respect workspace override")
  void notebooks() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resources create ai-notebook --name=$name`
    String name = "notebooks";
    TestCommand.runCommandExpectSuccess(
        "resources", "create", "ai-notebook", "--name=" + name, "--workspace=" + workspace2.id);
    pollDescribeForNotebookState(name, "PROVISIONING", workspace2.id);

    // `terra resources list --type=AI_NOTEBOOK --workspace=$id2`
    UFAiNotebook matchedNotebook = listOneNotebookResourceWithName(name, workspace2.id);
    assertEquals(name, matchedNotebook.name, "list output for workspace 2 matches notebook name");

    // `terra resources list --type=AI_NOTEBOOK`
    List<UFAiNotebook> matchedNotebooks = listNotebookResourcesWithName(name);
    assertEquals(0, matchedNotebooks.size(), "list output for notebooks in workspace 1 is empty");

    // `terra notebooks start --name=$name`
    // TODO (PF-869): change this to expect success and remove polling (state change is covered by
    // ctrl notebooks tests)
    TestCommand.runCommand("notebooks", "start", "--name=" + name, "--workspace=" + workspace2.id);
    pollDescribeForNotebookState(name, "ACTIVE", workspace2.id);

    // `terra notebooks stop --name=$name`
    // TODO (PF-869): change this to expect success and remove polling (state change is covered by
    // ctrl notebooks tests)
    TestCommand.runCommand("notebooks", "stop", "--name=" + name, "--workspace=" + workspace2.id);
    pollDescribeForNotebookState(name, "STOPPED", workspace2.id);
  }
}
