package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.dataproc.ClusterName;
import bio.terra.workspace.model.AccessScope;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.GcpDataprocClusterUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled GCP clusters. */
@Tag("unit-gcp")
public class GcpDataprocClusterControlled extends SingleWorkspaceUnitGcp {

  private UFGcpDataprocCluster testCluster;
  private final String name =
      "cliTestUserDataprocCluster" + UUID.randomUUID().toString().substring(0, 8);
  private final String stagingBucketName =
      "staging-bucket-" + UUID.randomUUID().toString().substring(0, 8);
  private final String tempBucketName =
      "temp-bucket-" + UUID.randomUUID().toString().substring(0, 8);

  // Create controlled cluster resource to use for all tests in this class.
  @BeforeAll
  void setupCluster() throws Exception {
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // Create staging and temp buckets needed by cluster
    UFGcsBucket stagingBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + stagingBucketName,
            "--bucket-name=" + stagingBucketName);

    UFGcsBucket tempBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + tempBucketName,
            "--bucket-name=" + tempBucketName);

    testCluster =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class,
            "resource",
            "create",
            "gcp-dataproc-cluster",
            "--name=" + name,
            "--bucket=" + stagingBucket.bucketName,
            "--temp-bucket=" + tempBucket.bucketName,
            "--metadata=foo=bar",
            "--idle-delete-ttl=1800s");

    GcpDataprocClusterUtils.pollDescribeForClusterState(name, "RUNNING");
  }

  @Test
  @DisplayName("list and describe reflect creating a controlled cluster")
  void listDescribeReflectCreate() throws IOException {
    workspaceCreator.login();
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // gcp clusters are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, testCluster.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        testCluster.privateUserName.toLowerCase(),
        "create output matches private user name");

    // check that the cluster is in the list
    UFGcpDataprocCluster matchedResource =
        GcpDataprocClusterUtils.listOneClusterResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");

    // `terra resource describe --name=$name --format=json`
    UFGcpDataprocCluster describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class, "resource", "describe", "--name=" + name);

    // check that the name matches and the instance id is populated
    assertEquals(name, describeResource.name, "describe resource output matches name");
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
            "%s/regions/%s/clusters/%s",
            testCluster.projectId, testCluster.region, testCluster.clusterId);
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertEquals(expectedResolved, resolved.get(name), "resolve returns the cluster id");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
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
    // synced. This works because we give "clusters.instances.getIamPolicy" to cluster editors.
    // The UFGcpDataprocCluster object is not fully populated at creation time, so we need an
    // additional `describe` call here.
    UFGcpDataprocCluster createdCluster =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpDataprocCluster.class, "resource", "describe", "--name=" + name);
    CrlUtils.callGcpWithPermissionExceptionRetries(
        () ->
            CrlUtils.createDataprocCow(workspaceCreator.getPetSaCredentials())
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
    TestCommand.runCommandExpectSuccessWithRetries("cluster", "stop", "--name=" + name);
    GcpDataprocClusterUtils.pollDescribeForClusterState(name, "STOPPED");

    // `terra cluster start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("cluster", "start", "--name=" + name);
    GcpDataprocClusterUtils.pollDescribeForClusterState(name, "RUNNING");
  }

  @Test
  @DisplayName("list and describe reflect a deleting cluster")
  void listDescribeReflectDelete() throws IOException {
    workspaceCreator.login();
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cluster delete --name=$name`
    TestCommand.Result cmd =
        TestCommand.runCommand("resource", "delete", "--name=" + name, "--quiet");
    // TODO (PF-745): use long-running job commands here
    boolean cliTimedOut =
        cmd.exitCode == 1
            && cmd.stdErr.contains(
                "CLI timed out waiting for the job to complete. It's still running on the server.");
    assertTrue(cmd.exitCode == 0 || cliTimedOut, "delete either succeeds or times out");

    if (!cliTimedOut) {
      // confirm it no longer appears in the resources list
      List<UFGcpDataprocCluster> listedClusters =
          GcpDataprocClusterUtils.listClusterResourcesWithName(name);
      assertThat(
          "deleted cluster no longer appears in the resources list",
          listedClusters,
          Matchers.empty());
    }
  }

  //
  // @Test
  // @DisplayName("override the default region and cluster id of a controlled cluster")
  // void overrideLocationAndInstanceId() throws IOException {
  //   workspaceCreator.login();
  //
  //   // `terra workspace set --id=$id`
  //   TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
  //
  //   // `terra resource create gcp-cluster --name=$name
  //   // --description=$description
  //   // --location=$location --instance-id=$instanceId`
  //   String name = "overrideLocationAndInstanceId";
  //   String description = "\"override default location and instance id\"";
  //   String region = "us-central1";
  //   String clusterId = "a" + UUID.randomUUID(); // instance id must start with a letter
  //   UFGcpDataprocCluster createdCluster =
  //       TestCommand.runAndParseCommandExpectSuccess(
  //           UFGcpDataprocCluster.class,
  //           "resource",
  //           "create",
  //           "gcp-cluster",
  //           "--name=" + name,
  //           "--description=" + description,
  //           "--region=" + region,
  //           "--cluster-id=" + clusterId);
  //
  //   // check that the properties match
  //   assertEquals(name, createdCluster.name, "create output matches name");
  //   assertEquals(description, createdCluster.description, "create output matches description");
  //   assertEquals(region, createdCluster.region, "create output matches location");
  //   assertEquals(clusterId, createdCluster.clusterId, "create output matches cluster id");
  //
  //   // gcp clusters are always private, no clone support
  //   assertEquals(
  //       AccessScope.PRIVATE_ACCESS, createdCluster.accessScope, "create output matches access");
  //   assertEquals(
  //       workspaceCreator.email.toLowerCase(),
  //       createdCluster.privateUserName.toLowerCase(),
  //       "create output matches private user name");
  //   assertEquals(
  //       CloningInstructionsEnum.NOTHING,
  //       createdCluster.cloningInstructions,
  //       "create output matches cloning instruction");
  //
  //   // `terra resource describe --name=$name --format=json`
  //   UFGcpDataprocCluster describeResource =
  //       TestCommand.runAndParseCommandExpectSuccess(
  //           UFGcpDataprocCluster.class, "resource", "describe", "--name=" + name);
  //
  //   // check that the properties match
  //   assertEquals(name, describeResource.name, "describe resource output matches name");
  //   assertEquals(description, describeResource.description, "describe output matches
  // description");
  //   assertEquals(region, describeResource.region, "describe resource output matches location");
  //   assertEquals(
  //       clusterId, describeResource.clusterId, "describe resource output matches instance id");
  //
  //   // gcp clusters are always private
  //   assertEquals(
  //       AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches
  // access");
  //   assertEquals(
  //       workspaceCreator.email.toLowerCase(),
  //       describeResource.privateUserName.toLowerCase(),
  //       "describe output matches private user name");
  //
  //   // new key-value pair will be appended, existing key-value pair will be updated.
  //   String newName = "NewOverrideLocationAndInstanceId";
  //   String newDescription = "\"new override default location and instance id\"";
  //   String newKey1 = "NewMetadata1";
  //   String newKey2 = "NewMetadata2";
  //   String newValue1 = "metadata1";
  //   String newValue2 = "metadata2";
  //   String newEntry1 = newKey1 + "=" + newValue1;
  //   String newEntry2 = newKey2 + "=" + newValue2;
  //   UFGcpDataprocCluster updatedNotebook =
  //       TestCommand.runAndParseCommandExpectSuccess(
  //           UFGcpDataprocCluster.class,
  //           "resource",
  //           "update",
  //           "gcp-cluster",
  //           "--name=" + name,
  //           "--new-name=" + newName,
  //           "--new-description=" + newDescription,
  //           "--new-metadata=" + newEntry1 + "," + newEntry2);
  //
  //   // check that the properties match
  //   // the metadata supports multiple entries, we can't assert on the metadata because it's not
  //   // stored in or accessible via Workspace Manager.
  //   assertEquals(newName, updatedNotebook.name, "create output matches name");
  //   assertEquals(newDescription, updatedNotebook.description, "create output matches
  // description");
  //   assertEquals(
  //       newValue1,
  //       updatedNotebook.metadata.get(newKey1),
  //       "create output matches metadata" + newKey1 + ": " + newValue1);
  //   assertEquals(
  //       newValue2,
  //       updatedNotebook.metadata.get(newKey2),
  //       "create output matches metadata" + newKey2 + ": " + newValue2);
  // }
}
