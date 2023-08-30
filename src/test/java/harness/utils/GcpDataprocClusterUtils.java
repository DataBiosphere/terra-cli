package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.util.List;
import java.util.stream.Collectors;

public class GcpDataprocClusterUtils {
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
