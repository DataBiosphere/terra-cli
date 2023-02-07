package harness.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

public class AwsNotebookUtils {
  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Uses the current workspace.
   */
  public static void assertNotebookState(String resourceName, NotebookInstanceStatus notebookState)
      throws JsonProcessingException {
    assertNotebookState(resourceName, notebookState, null);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static void assertNotebookState(
      String resourceName, NotebookInstanceStatus notebookState, String workspaceUserFacingId)
      throws JsonProcessingException {
    UFAwsNotebook describeNotebook =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                UFAwsNotebook.class, "resource", "describe", "--name=" + resourceName)
            : TestCommand.runAndParseCommandExpectSuccess(
                UFAwsNotebook.class,
                "resource",
                "describe",
                "--name=" + resourceName,
                "--workspace=" + workspaceUserFacingId);
    // TODO(TERRA-368)
    // assertEquals(notebookState, describeNotebook.state, "notebook state matches");
    // if (!notebookState.equals("PROVISIONING")) {
    //  assertNotNull(describeNotebook.proxyUri, "proxy url is populated");
    // }
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  public static UFAwsNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneNotebookResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  public static UFAwsNotebook listOneNotebookResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFAwsNotebook> matchedResources =
        listNotebookResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  public static List<UFAwsNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listNotebookResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  public static List<UFAwsNotebook> listNotebookResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=AWS_SAGEMAKER_NOTEBOOK --format=json`
    List<UFAwsNotebook> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AWS_SAGEMAKER_NOTEBOOK")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AWS_SAGEMAKER_NOTEBOOK",
                "--workspace=" + workspaceUserFacingId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
