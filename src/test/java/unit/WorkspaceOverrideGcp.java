package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static unit.BqDatasetControlled.listDatasetResourcesWithName;
import static unit.BqDatasetControlled.listOneDatasetResourceWithName;
import static unit.GcsBucketControlled.listBucketResourcesWithName;
import static unit.GcsBucketControlled.listOneBucketResourceWithName;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.utils.CrlUtils;
import com.google.api.gax.paging.Page;
import com.google.api.services.bigquery.model.DatasetReference;
import com.google.cloud.Identity;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import harness.TestCommand;
import harness.TestContext;
import harness.baseclasses.ClearContextUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import harness.utils.ExternalGCSBuckets;
import harness.utils.GcpNotebookUtils;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the `--workspace` option to override the current GCP workspace just for this command.
 */
@Tag("unit-gcp")
public class WorkspaceOverrideGcp extends ClearContextUnit {
  private static UFWorkspace workspace1;
  private static UFWorkspace workspace2;

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalBucket;

  // external dataset to use for creating BQ dataset references in the workspace
  private DatasetReference externalDataset;

  /**
   * Create two workspaces for tests to use, so we can switch between them with the override option.
   */
  @Override
  @BeforeAll
  protected void setupOnce() throws Exception {
    super.setupOnce();

    // grant the user's proxy group access to the bucket and dataset so that they will pass WSM's
    // access check when adding them as referenced resources
    externalBucket = ExternalGCSBuckets.createBucketWithUniformAccess();
    ExternalGCSBuckets.grantReadAccess(externalBucket, Identity.user(workspaceCreator.email));
    ExternalGCSBuckets.grantReadAccess(externalBucket, Identity.group(Auth.getProxyGroupEmail()));
    externalDataset = ExternalBQDatasets.createDataset();
    ExternalBQDatasets.grantReadAccess(
        externalDataset, workspaceCreator.email, ExternalBQDatasets.IamMemberType.USER);
    ExternalBQDatasets.grantReadAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);

    workspace1 = WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));
    workspace2 = WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));
  }

  /** Delete the two workspaces. */
  @AfterAll
  protected void cleanupOnce() throws IOException, GeneralSecurityException {
    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace2.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    ExternalGCSBuckets.deleteBucket(externalBucket);
    externalBucket = null;
    ExternalBQDatasets.deleteDataset(externalDataset);
    externalDataset = null;
  }

  @Test
  @DisplayName("referenced resource commands respect workspace override")
  void referencedResourcesGcp() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resource add-ref gcs-bucket --name=$resourceNameBucket --bucket-name=$bucketName
    // --workspace=$id2`
    String resourceNameBucket = "referencedResources_bucket";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + resourceNameBucket,
        "--bucket-name=" + externalBucket.getName(),
        "--workspace=" + workspace2.id);

    // `terra resource add-ref bq-dataset --name=$resourceNameDataset --dataset-id=$datasetId
    // --workspace=$id2`
    String resourceNameDataset = "referencedResources_dataset";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + resourceNameDataset,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--workspace=" + workspace2.id);

    // check that workspace 2 contains both referenced resources above and workspace 1 does not

    // `terra resource list --type=GCS_BUCKET --workspace=$id2`
    UFGcsBucket matchedBucket = listOneBucketResourceWithName(resourceNameBucket, workspace2.id);
    assertEquals(
        resourceNameBucket, matchedBucket.name, "list output for workspace 2 matches bucket name");

    // `terra resource list --type=GCS_BUCKET`
    List<UFGcsBucket> matchedBuckets = listBucketResourcesWithName(resourceNameBucket);
    assertEquals(0, matchedBuckets.size(), "list output for bucket in workspace 1 is empty");

    // `terra resource list --type=BQ_DATASET --workspace=$id2`
    UFBqDataset matchedDataset = listOneDatasetResourceWithName(resourceNameDataset, workspace2.id);
    assertEquals(
        resourceNameDataset,
        matchedDataset.name,
        "list output for workspace 2 matches dataset name");

    // `terra resource list --type=BQ_DATASET`
    List<UFBqDataset> matchedDatasets = listDatasetResourcesWithName(resourceNameDataset);
    assertEquals(0, matchedDatasets.size(), "list output for dataset in workspace 1 is empty");

    // check that check-access, describe, and resolve succeed in workspace 2 but not in workspace 1

    // `terra check-access --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resource", "check-access", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resource", "check-access", "--name=" + resourceNameBucket);

    // `terra describe --name=$resourceNameDataset`
    TestCommand.runCommandExpectSuccess(
        "resource", "describe", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resource", "describe", "--name=" + resourceNameDataset);

    // `terra resolve --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resource", "resolve", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(1, "resource", "resolve", "--name=" + resourceNameBucket);

    // `terra delete --name=$resourceNameBucket --workspace=$id2`
    TestCommand.runCommandExpectExitCode(
        1, "resource", "delete", "--name=" + resourceNameBucket, "--quiet");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "delete",
        "--name=" + resourceNameBucket,
        "--workspace=" + workspace2.id,
        "--quiet");

    // `terra delete --name=$resourceNameDataset --workspace=$id2`
    TestCommand.runCommandExpectExitCode(
        1, "resource", "delete", "--name=" + resourceNameDataset, "--quiet");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "delete",
        "--name=" + resourceNameDataset,
        "--workspace=" + workspace2.id,
        "--quiet");
  }

  @Test
  @DisplayName("controlled resource commands respect workspace override")
  void controlledResourcesGcp() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resource create gcs-bucket --name=$resourceNameBucket --bucket-name=$bucketName
    // --workspace=$id2`
    String resourceNameBucket = "controlledResources_bucket";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "gcs-bucket",
        "--name=" + resourceNameBucket,
        "--bucket-name=" + bucketName,
        "--workspace=" + workspace2.id);

    // `terra resource create bq-dataset --name=$resourceNameDataset --dataset-id=$datasetId
    // --workspace=$id2`
    String resourceNameDataset = "controlledResources_dataset";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "bq-dataset",
        "--name=" + resourceNameDataset,
        "--dataset-id=" + datasetId,
        "--workspace=" + workspace2.id);

    // check that workspace 2 contains both controlled resources above and workspace 1 does not

    // `terra resource list --type=GCS_BUCKET --workspace=$id2`
    UFGcsBucket matchedBucket = listOneBucketResourceWithName(resourceNameBucket, workspace2.id);
    assertEquals(
        resourceNameBucket, matchedBucket.name, "list output for workspace 2 matches bucket name");

    // `terra resource list --type=GCS_BUCKET`
    List<UFGcsBucket> matchedBuckets = listBucketResourcesWithName(resourceNameBucket);
    assertEquals(0, matchedBuckets.size(), "list output for bucket in workspace 1 is empty");

    // `terra resource list --type=BQ_DATASET --workspace=$id2`
    UFBqDataset matchedDataset = listOneDatasetResourceWithName(resourceNameDataset, workspace2.id);
    assertEquals(
        resourceNameDataset,
        matchedDataset.name,
        "list output for workspace 2 matches dataset name");

    // `terra resource list --type=BQ_DATASET`
    List<UFBqDataset> matchedDatasets = listDatasetResourcesWithName(resourceNameDataset);
    assertEquals(0, matchedDatasets.size(), "list output for dataset in workspace 1 is empty");

    // check that describe and resolve succeed in workspace 2 but not in workspace 1

    // `terra describe --name=$resourceNameDataset`
    TestCommand.runCommandExpectSuccess(
        "resource", "describe", "--name=" + resourceNameDataset, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(
        1, "resource", "describe", "--name=" + resourceNameDataset);

    // `terra resolve --name=$resourceNameBucket`
    TestCommand.runCommandExpectSuccess(
        "resource", "resolve", "--name=" + resourceNameBucket, "--workspace=" + workspace2.id);
    TestCommand.runCommandExpectExitCode(1, "resource", "resolve", "--name=" + resourceNameBucket);

    // `terra delete --name=$resourceNameBucket --workspace=$id2`
    TestCommand.runCommandExpectExitCode(
        1, "resource", "delete", "--name=" + resourceNameBucket, "--quiet");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "delete",
        "--name=" + resourceNameBucket,
        "--workspace=" + workspace2.id,
        "--quiet");

    // `terra delete --name=$resourceNameDataset --workspace=$id2`
    TestCommand.runCommandExpectExitCode(
        1, "resource", "delete", "--name=" + resourceNameDataset, "--quiet");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "delete",
        "--name=" + resourceNameDataset,
        "--workspace=" + workspace2.id,
        "--quiet");
  }

  @Test
  @DisplayName("notebook commands respect workspace override")
  void notebooksGcp() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra resources create gcp-notebook --name=$name`
    String name = "notebooks";
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcp-notebook", "--name=" + name, "--workspace=" + workspace2.id);

    // Poll until the test user can list GCS buckets in the workspace project, which may be delayed.
    // This is a hack to get around IAM permission delay.
    Storage localProjectStorageClient =
        StorageOptions.newBuilder()
            .setProjectId(workspace2.googleProjectId)
            .setCredentials(workspaceCreator.getCredentialsWithCloudPlatformScope())
            .build()
            .getService();
    Page<Bucket> createdBucketOnCloud =
        CrlUtils.callGcpWithPermissionExceptionRetries(localProjectStorageClient::list);

    GcpNotebookUtils.pollDescribeForNotebookState(name, "ACTIVE", workspace2.id);

    // `terra resources list --type=AI_NOTEBOOK --workspace=$id2`
    UFGcpNotebook matchedNotebook =
        GcpNotebookUtils.listOneNotebookResourceWithName(name, workspace2.id);
    assertEquals(name, matchedNotebook.name, "list output for workspace 2 matches notebook name");

    // `terra resources list --type=AI_NOTEBOOK`
    List<UFGcpNotebook> matchedNotebooks = GcpNotebookUtils.listNotebookResourcesWithName(name);
    assertEquals(0, matchedNotebooks.size(), "list output for notebooks in workspace 1 is empty");

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries(
        "notebook", "start", "--name=" + name, "--workspace=" + workspace2.id);

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries(
        "notebook", "stop", "--name=" + name, "--workspace=" + workspace2.id);
  }
}
