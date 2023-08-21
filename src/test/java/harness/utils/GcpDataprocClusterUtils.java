package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.service.utils.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class GcpDataprocClusterUtils {
  /**
   * Helper method to poll `terra resources describe` until the cluster state equals that specified.
   * Uses the current workspace.
   */
  public static void pollDescribeForClusterState(String resourceName, String clusterState)
      throws InterruptedException, JsonProcessingException {
    pollDescribeForClusterState(resourceName, clusterState, null);
  }

  /**
   * Helper method to poll `terra resources describe` until the cluster state equals that specified.
   * Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static void pollDescribeForClusterState(
      String resourceName, String clusterState, String workspaceUserFacingId)
      throws InterruptedException, JsonProcessingException {
    HttpUtils.pollWithRetries(
        () ->
            workspaceUserFacingId == null
                ? TestCommand.runAndParseCommandExpectSuccess(
                    UFGcpDataprocCluster.class, "resource", "describe", "--name=" + resourceName)
                : TestCommand.runAndParseCommandExpectSuccess(
                    UFGcpDataprocCluster.class,
                    "resource",
                    "describe",
                    "--name=" + resourceName,
                    "--workspace=" + workspaceUserFacingId),
        (result) -> clusterState.equals(result.status),
        (ex) -> false, // no retries
        2 * 20, // up to 20 minutes
        Duration.ofSeconds(30)); // every 30 seconds

    assertClusterState(resourceName, clusterState, workspaceUserFacingId);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the cluster state matches that
   * given. Uses the current workspace.
   */
  public static void assertClusterState(String resourceName, String clusterState)
      throws JsonProcessingException {
    assertClusterState(resourceName, clusterState, null);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the cluster state matches that
   * given. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static void assertClusterState(
      String resourceName, String clusterState, String workspaceUserFacingId)
      throws JsonProcessingException {
    UFGcpDataprocCluster describeCluster =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                UFGcpDataprocCluster.class, "resource", "describe", "--name=" + resourceName)
            : TestCommand.runAndParseCommandExpectSuccess(
                UFGcpDataprocCluster.class,
                "resource",
                "describe",
                "--name=" + resourceName,
                "--workspace=" + workspaceUserFacingId);
    assertEquals(clusterState, describeCluster.status, "cluster state matches");
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFGcpDataprocCluster listOneClusterResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneClusterResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFGcpDataprocCluster listOneClusterResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFGcpDataprocCluster> matchedResources =
        listClusterResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFGcpDataprocCluster> listClusterResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listClusterResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static List<UFGcpDataprocCluster> listClusterResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=DATAPROC_CLUSTER --format=json`
    List<UFGcpDataprocCluster> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=DATAPROC_CLUSTER")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=DATAPROC_CLUSTER",
                "--workspace=" + workspaceUserFacingId);

    // find the matching cluster in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
