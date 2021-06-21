package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.resources.UFAiNotebook;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resources` commands that handle controlled AI notebooks. */
@Tag("unit")
public class AiNotebookControlled extends SingleWorkspaceUnit {
  @Test
  @DisplayName("list and describe reflect creating and deleting a controlled notebook")
  void listDescribeReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create ai-notebook --name=$name`
    String name = "listDescribeReflectCreateDelete";
    UFAiNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resources", "create", "ai-notebook", "--name=" + name);

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

    // `terra resources describe --name=$name --format=json`
    UFAiNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resources", "describe", "--name=" + name);

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

    // `terra notebooks delete --name=$name`
    TestCommand.Result cmd = TestCommand.runCommand("resources", "delete", "--name=" + name);
    assertThat(
        "delete either succeeds or times out",
        cmd.exitCode == 0
            || (cmd.exitCode == 1
                && cmd.stdErr.contains(
                    "CLI timed out waiting for the job to complete. It's still running on the server.")));

    // confirm it no longer appears in the resources list
    List<UFAiNotebook> listedNotebooks = listNotebookResourcesWithName(name);
    assertEquals(
        0, listedNotebooks.size(), "deleted notebook no longer appears in the resources list");
  }

  @Test
  @DisplayName("resolve and check-access for a controlled notebook")
  void resolveAndCheckAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create ai-notebook --name=$name`
    String name = "resolveAndCheckAccess";
    UFAiNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resources", "create", "ai-notebook", "--name=" + name);

    // `terra resources resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name);
    assertEquals(createdNotebook.instanceName, resolved, "resolve returns the instance name");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resources", "check-access", "--name=" + name);
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

    // `terra resources create ai-notebook --name=$name
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
            "resources",
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

    // `terra resources describe --name=$name --format=json`
    UFAiNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resources", "describe", "--name=" + name);

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
    // `terra resources create ai-notebook --name=$name`
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create ai-notebook --name=$name`
    String name = "startStop";
    TestCommand.runCommandExpectSuccess("resources", "create", "ai-notebook", "--name=" + name);
    pollDescribeForNotebookState(name, "PROVISIONING");

    // `terra notebooks start --name=$name`
    // TODO (PF-869): change this to expect success once polling notebook operations is fixed
    //    TestCommand.runCommandExpectSuccess("notebooks", "start", "--name=" + name);
    TestCommand.runCommand("notebooks", "start", "--name=" + name);
    pollDescribeForNotebookState(name, "ACTIVE");

    // `terra notebooks stop --name=$name`
    // TODO (PF-869): change this to expect success once polling notebook operations is fixed
    //    TestCommand.runCommandExpectSuccess("notebooks", "stop", "--name=" + name);
    TestCommand.runCommand("notebooks", "stop", "--name=" + name);
    pollDescribeForNotebookState(name, "STOPPED");
  }

  /**
   * Helper method to poll `terra resources describe` until the notebook state equals that
   * specified.
   */
  private void pollDescribeForNotebookState(String resourceName, String notebookState)
      throws InterruptedException, JsonProcessingException {
    HttpUtils.pollWithRetries(
        () ->
            TestCommand.runAndParseCommandExpectSuccess(
                UFAiNotebook.class, "resources", "describe", "--name=" + resourceName),
        (result) -> notebookState.equals(result.state),
        (ex) -> false, // no retries
        2 * 20, // up to 20 minutes
        Duration.ofSeconds(30)); // every 30 seconds

    UFAiNotebook describeNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFAiNotebook.class, "resources", "describe", "--name=" + resourceName);
    assertEquals(notebookState, describeNotebook.state, "notebook state matches");
    if (!notebookState.equals("PROVISIONING")) {
      assertNotNull(describeNotebook.proxyUri, "proxy url is populated");
    }
  }

  /** Helper method to call `terra resources list` and expect one resource with this name. */
  static UFAiNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    List<UFAiNotebook> matchedResources = listNotebookResourcesWithName(resourceName);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name.
   */
  static List<UFAiNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=AI_NOTEBOOK --format=json`
    List<UFAiNotebook> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resources", "list", "--type=AI_NOTEBOOK");

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
