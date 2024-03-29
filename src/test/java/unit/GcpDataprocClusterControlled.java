package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.FeatureService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.dataproc.ClusterName;
import bio.terra.workspace.model.AccessScope;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.GcpDataprocClusterUtils;
import harness.utils.ResourceUtils;
import harness.utils.TestCrlUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the `terra resource` commands that handle controlled GCP clusters.
 *
 * <p>These tests share a single controlled cluster resource created before the tests run.
 * Additional tests must be added before {@link #listDescribeReflectDelete()}
 */
@Tag("unit-gcp")
public class GcpDataprocClusterControlled extends SingleWorkspaceUnitGcp {

  private UFGcpDataprocCluster createdCluster;
  private final String randomUuid8 = UUID.randomUUID().toString().substring(0, 8);
  private final String clusterName = "cliTestUserDataprocCluster-" + randomUuid8;
  private final String stagingBucketName = "staging-bucket-" + randomUuid8;
  private final String tempBucketName = "temp-bucket-" + randomUuid8;
  private UFGcsBucket stagingBucket;
  private UFGcsBucket tempBucket;

  // Create controlled cluster resource to use for all tests in this class.
  @BeforeAll
  void setupCluster() throws Exception {
    // Only run this test suite if dataproc clusters are enabled in the environment
    Assumptions.assumeTrue(
        FeatureService.fromContext().isFeatureEnabled(FeatureService.CLI_DATAPROC_ENABLED),
        "Dataproc is not supported in the current environment. Skipping tests.");

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // Create staging and temp buckets needed by cluster
    stagingBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + stagingBucketName,
            "--bucket-name=" + stagingBucketName);

    tempBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + tempBucketName,
            "--bucket-name=" + tempBucketName);

    createdCluster =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class,
            "resource",
            "create",
            "dataproc-cluster",
            "--name=" + clusterName,
            "--bucket=" + stagingBucket.bucketName,
            "--temp-bucket=" + tempBucket.bucketName,
            "--metadata=foo=bar",
            "--idle-delete-ttl=1800s");

    ResourceUtils.pollDescribeForResourceField(clusterName, "status", "RUNNING");
  }

  @Test
  @DisplayName("list and describe reflect creating a controlled cluster")
  void listDescribeReflectCreate() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // gcp clusters are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdCluster.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdCluster.privateUserName.toLowerCase(),
        "create output matches private user name");

    // check that the cluster is in the list
    UFGcpDataprocCluster matchedResource =
        GcpDataprocClusterUtils.listOneClusterResourceWithName(clusterName);
    assertEquals(clusterName, matchedResource.name, "list output matches name");

    // `terra resource describe --name=$name --format=json`
    UFGcpDataprocCluster describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class, "resource", "describe", "--name=" + clusterName);

    // check that the name matches and the cluster id is populated
    assertEquals(clusterName, describeResource.name, "describe resource output matches name");
    assertNotNull(describeResource.clusterId, "describe resource output includes cluster id");

    // gcp clusters are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");
  }

  @Test
  @DisplayName("resolve and check-access for a controlled cluster")
  void resolveAndCheckAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource resolve --name=$name --format=json`
    String expectedResolved =
        String.format(
            "projects/%s/regions/%s/clusters/%s",
            createdCluster.projectId, createdCluster.region, createdCluster.clusterId);
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + clusterName);
    assertEquals(expectedResolved, resolved.get(clusterName), "resolve returns the cluster id");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "check-access", "--name=" + clusterName);
    assertThat(
        "check-access error because gcp clusters are controlled resources",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));
  }

  @Test // NOTE: This test takes ~10 minutes to run.
  @DisplayName("start, stop a cluster and poll until they complete")
  void startStop() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // Poll until the test user can get the cluster IAM bindings directly to confirm cloud
    // permissions have
    // synced. This works because we give "dataproc.clusters.getIamPolicy" to cluster editors.
    // The UFGcpDataprocCluster object is not fully populated at creation time, so we need an
    // additional `describe` call here.
    UFGcpDataprocCluster createdCluster =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class, "resource", "describe", "--name=" + clusterName);
    CrlUtils.callGcpWithPermissionExceptionRetries(
        () ->
            TestCrlUtils.createDataprocCow(workspaceCreator.getPetSaCredentials())
                .clusters()
                .getIamPolicy(
                    ClusterName.builder()
                        .projectId(createdCluster.projectId)
                        .region(createdCluster.region)
                        .name(createdCluster.clusterId)
                        .build())
                .execute(),
        Objects::nonNull);

    // `terra cluster stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("cluster", "stop", "--name=" + clusterName);
    ResourceUtils.pollDescribeForResourceField(clusterName, "status", "STOPPED");

    // `terra cluster start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("cluster", "start", "--name=" + clusterName);
    ResourceUtils.pollDescribeForResourceField(clusterName, "status", "RUNNING");
  }

  @Test
  @DisplayName("launch cluster jupyterlab proxy url")
  void launchProxy() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cluster launch --name=$name --proxy-view=JUPYTER_LAB --format=json`
    JSONObject proxyUrl =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "cluster", "launch", "--name=" + clusterName, "--proxy-view=JUPYTER_LAB");
    String jupyterProxyViewUrl = proxyUrl.getString("JupyterLab");
    assertNotNull(jupyterProxyViewUrl, "launch cluster jupyterlab proxy url is not null");
  }

  @Test
  @DisplayName("update cluster worker count and idle deletion time")
  void update() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // Update wsm metadata and worker counts
    String newDescription = "\"new cluster description\"";
    int newPrimaryWorkerCount = 3;
    int newSecondaryWorkerCount = 3;
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcpDataprocCluster.class,
        "resource",
        "update",
        "dataproc-cluster",
        "--name=" + clusterName,
        "--new-description=" + newDescription,
        "--num-workers=" + newPrimaryWorkerCount,
        "--num-secondary-workers=" + newSecondaryWorkerCount);
    UFGcpDataprocCluster updatedCluster;

    // While the cluster is scaling down secondary workers (takes ~30 seconds), ensure that
    // additional updates throw a user facing exception
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1,
            "resource",
            "update",
            "dataproc-cluster",
            "--name=" + clusterName,
            "--num-secondary-workers=5");
    assertThat(
        "error message includes expected and current status",
        stdErr,
        CoreMatchers.containsString("Expected cluster status is"));

    // Poll until the cluster is running again
    ResourceUtils.pollDescribeForResourceField(clusterName, "status", "RUNNING");

    // Update idle deletion time
    String newIdleDeleteTtl = "2000s";
    updatedCluster =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class,
            "resource",
            "update",
            "dataproc-cluster",
            "--name=" + clusterName,
            "--idle-delete-ttl=" + newIdleDeleteTtl);
    ResourceUtils.pollDescribeForResourceField(clusterName, "status", "RUNNING");

    // check that the fields are correctly updated
    assertEquals(
        newDescription, updatedCluster.description, "cluster description matches expected");

    UFGcpDataprocCluster describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class, "resource", "describe", "--name=" + clusterName);

    assertEquals(
        newPrimaryWorkerCount,
        describeResource.numWorkers,
        "cluster num primary workers matches expected");
    assertEquals(
        newSecondaryWorkerCount,
        describeResource.numSecondaryWorkers,
        "cluster num secondary workers matches expected");
    assertEquals(
        newIdleDeleteTtl,
        describeResource.idleDeleteTtl,
        "cluster idle delete ttl matches expected");
  }

  @Test
  @DisplayName("list and describe reflect a deleting cluster")
  void listDescribeReflectDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cluster delete --name=$name`
    TestCommand.Result cmd =
        TestCommand.runCommand("resource", "delete", "--name=" + clusterName, "--quiet");
    // TODO (PF-745): use long-running job commands here
    boolean cliTimedOut =
        cmd.exitCode == 1
            && cmd.stdErr.contains(
                "CLI timed out waiting for the job to complete. It's still running on the server.");
    assertTrue(cmd.exitCode == 0 || cliTimedOut, "delete either succeeds or times out");

    if (!cliTimedOut) {
      // confirm it no longer appears in the resources list
      List<UFGcpDataprocCluster> listedClusters =
          GcpDataprocClusterUtils.listClusterResourcesWithName(clusterName);
      assertThat(
          "deleted cluster no longer appears in the resources list",
          listedClusters,
          Matchers.empty());
    }
  }
}
