package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFAiNotebook;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled AI notebooks. */
@Tag("unit")
public class AiNotebookControlled extends SingleWorkspaceUnit {
  @Test
  @DisplayName("list and describe reflect creating and deleting a controlled notebook")
  void listDescribeReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create ai-notebook --name=$name`
    String name = "listDescribeReflectCreateDelete";
    UFAiNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resource", "create", "ai-notebook", "--name=" + name);

    // check that the name and notebook name match
    assertEquals(name, createdNotebook.name, "create output matches name");

    // ai notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them

    // check that the notebook is in the list
    UFAiNotebook matchedResource = listOneNotebookResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");

    // `terra resource describe --name=$name --format=json`
    UFAiNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resource", "describe", "--name=" + name);

    // check that the name matches and the instance id is populated
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertNotNull(describeResource.instanceName, "describe resource output includes instance name");

    // ai notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them

    // `terra notebook delete --name=$name`
    TestCommand.Result cmd =
        TestCommand.runCommand("resource", "delete", "--name=" + name, "--quiet");
    assertTrue(
        cmd.exitCode == 0
            || (cmd.exitCode == 1
                && cmd.stdErr.contains(
                    "CLI timed out waiting for the job to complete. It's still running on the server.")),
        "delete either succeeds or times out");

    // confirm it no longer appears in the resources list
    List<UFAiNotebook> listedNotebooks = listNotebookResourcesWithName(name);
    assertThat(
        "deleted notebook no longer appears in the resources list",
        listedNotebooks,
        Matchers.empty());
  }

  @Test
  @DisplayName("resolve and check-access for a controlled notebook")
  void resolveAndCheckAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create ai-notebook --name=$name`
    String name = "resolveAndCheckAccess";
    UFAiNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resource", "create", "ai-notebook", "--name=" + name);

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(createdNotebook.instanceName, resolved, "resolve returns the instance name");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "check-access error is because ai notebooks are controlled resources",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));
  }

  @Test
  @DisplayName("override the default location and instance id")
  void overrideLocationAndInstanceId() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create ai-notebook --name=$name
    // --cloning=$cloning --description=$description
    // --location=$location --instance-id=$instanceId`
    String name = "overrideLocationAndInstanceId";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.REFERENCE;
    String description = "\"override default location and instance id\"";
    String location = "us-central1-b";
    String instanceId = "a" + UUID.randomUUID().toString(); // instance id must start with a letter
    UFAiNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class,
            "resource",
            "create",
            "ai-notebook",
            "--name=" + name,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location,
            "--instance-id=" + instanceId);

    // check that the properties match
    assertEquals(name, createdNotebook.name, "create output matches name");
    assertEquals(cloning, createdNotebook.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdNotebook.description, "create output matches description");
    assertEquals(location, createdNotebook.location, "create output matches location");
    assertEquals(instanceId, createdNotebook.instanceId, "create output matches instance id");

    // ai notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them

    // `terra resource describe --name=$name --format=json`
    UFAiNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(location, describeResource.location, "describe resource output matches location");
    assertEquals(
        instanceId, describeResource.instanceId, "describe resource output matches instance id");

    // ai notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them
  }

  @Test // NOTE: This test takes ~10 minutes to run.
  @DisplayName("start, stop a notebook and poll until they complete")
  void startStop() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create ai-notebook --name=$name`
    String name = "startStop";
    TestCommand.runCommandExpectSuccess("resource", "create", "ai-notebook", "--name=" + name);
    assertNotebookState(name, "PROVISIONING");
    pollDescribeForNotebookState(name, "ACTIVE");

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccess("notebook", "start", "--name=" + name);
    assertNotebookState(name, "ACTIVE");

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccess("notebook", "stop", "--name=" + name);
    assertNotebookState(name, "STOPPED");

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccess("notebook", "start", "--name=" + name);
    assertNotebookState(name, "ACTIVE");
  }

  /**
   * Helper method to poll `terra resources describe` until the notebook state equals that
   * specified. Uses the current workspace.
   */
  static void pollDescribeForNotebookState(String resourceName, String notebookState)
      throws InterruptedException, JsonProcessingException {
    pollDescribeForNotebookState(resourceName, notebookState, null);
  }

  /**
   * Helper method to poll `terra resources describe` until the notebook state equals that
   * specified. Filters on the specified workspace id; Uses the current workspace if null.
   */
  static void pollDescribeForNotebookState(
      String resourceName, String notebookState, UUID workspaceId)
      throws InterruptedException, JsonProcessingException {
    HttpUtils.pollWithRetries(
        () ->
            workspaceId == null
                ? TestCommand.runAndParseCommandExpectSuccess(
                    UFAiNotebook.class, "resource", "describe", "--name=" + resourceName)
                : TestCommand.runAndParseCommandExpectSuccess(
                    UFAiNotebook.class,
                    "resource",
                    "describe",
                    "--name=" + resourceName,
                    "--workspace=" + workspaceId),
        (result) -> notebookState.equals(result.state),
        (ex) -> false, // no retries
        2 * 20, // up to 20 minutes
        Duration.ofSeconds(30)); // every 30 seconds

    assertNotebookState(resourceName, notebookState, workspaceId);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Uses the current workspace.
   */
  private static void assertNotebookState(String resourceName, String notebookState)
      throws JsonProcessingException {
    assertNotebookState(resourceName, notebookState, null);
  }

  /**
   * Helper method to call `terra resource describe` and assert that the notebook state matches that
   * given. Filters on the specified workspace id; Uses the current workspace if null.
   */
  static void assertNotebookState(String resourceName, String notebookState, UUID workspaceId)
      throws JsonProcessingException {
    UFAiNotebook describeNotebook =
        workspaceId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                UFAiNotebook.class, "resource", "describe", "--name=" + resourceName)
            : TestCommand.runAndParseCommandExpectSuccess(
                UFAiNotebook.class,
                "resource",
                "describe",
                "--name=" + resourceName,
                "--workspace=" + workspaceId);
    assertEquals(notebookState, describeNotebook.state, "notebook state matches");
    if (!notebookState.equals("PROVISIONING")) {
      assertNotNull(describeNotebook.proxyUri, "proxy url is populated");
    }
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  static UFAiNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneNotebookResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  static UFAiNotebook listOneNotebookResourceWithName(String resourceName, UUID workspaceId)
      throws JsonProcessingException {
    List<UFAiNotebook> matchedResources = listNotebookResourcesWithName(resourceName, workspaceId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  static List<UFAiNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listNotebookResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  static List<UFAiNotebook> listNotebookResourcesWithName(String resourceName, UUID workspaceId)
      throws JsonProcessingException {
    // `terra resources list --type=AI_NOTEBOOK --format=json`
    List<UFAiNotebook> listedResources =
        workspaceId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=AI_NOTEBOOK")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=AI_NOTEBOOK",
                "--workspace=" + workspaceId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
