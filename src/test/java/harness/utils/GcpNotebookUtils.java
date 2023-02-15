package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.cli.service.utils.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class GcpNotebookUtils {
  /**
   * Helper method to poll `terra resources describe` until the notebook state equals that
   * specified. Uses the current workspace.
   */
  public static void pollDescribeForNotebookState(String resourceName, String notebookState)
      throws InterruptedException, JsonProcessingException {
    pollDescribeForNotebookState(resourceName, notebookState, null);
  }

  /**
   * Helper method to poll `terra resources describe` until the notebook state equals that
   * specified. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static void pollDescribeForNotebookState(
      String resourceName, String notebookState, String workspaceUserFacingId)
      throws InterruptedException, JsonProcessingException {
    HttpUtils.pollWithRetries(
        () ->
            workspaceUserFacingId == null
                ? TestCommand.runAndParseCommandExpectSuccess(
                    UFGcpNotebook.class, "resource", "describe", "--name=" + resourceName)
                : TestCommand.runAndParseCommandExpectSuccess(
                    UFGcpNotebook.class,
                    "resource",
                    "describe",
                    "--name=" + resourceName,
                    "--workspace=" + workspaceUserFacingId),
        (result) -> notebookState.equals(result.state),
        (ex) -> false, // no retries
        4 * 20, // up to 20 minutes
        Duration.ofSeconds(15)); // every 15 seconds

    assertNotebookState(resourceName, notebookState, workspaceUserFacingId);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Uses the current workspace.
   */
  public static void assertNotebookState(String resourceName, String notebookState)
      throws JsonProcessingException {
    assertNotebookState(resourceName, notebookState, null);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static void assertNotebookState(
      String resourceName, String notebookState, String workspaceUserFacingId)
      throws JsonProcessingException {
    UFGcpNotebook describeNotebook =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                UFGcpNotebook.class, "resource", "describe", "--name=" + resourceName)
            : TestCommand.runAndParseCommandExpectSuccess(
                UFGcpNotebook.class,
                "resource",
                "describe",
                "--name=" + resourceName,
                "--workspace=" + workspaceUserFacingId);
    assertEquals(notebookState, describeNotebook.state, "notebook state matches");
    if (!notebookState.equals("PROVISIONING")) {
      assertNotNull(describeNotebook.proxyUri, "proxy url is populated");
    }
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFGcpNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneNotebookResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFGcpNotebook listOneNotebookResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFGcpNotebook> matchedResources =
        listNotebookResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFGcpNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listNotebookResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static List<UFGcpNotebook> listNotebookResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AI_NOTEBOOK --format=json`
    List<UFGcpNotebook> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AI_NOTEBOOK")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AI_NOTEBOOK",
                "--workspace=" + workspaceUserFacingId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
