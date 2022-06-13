package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
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

/** Tests for the `terra resource` commands that handle controlled GCP notebooks. */
@Tag("unit")
public class GcpNotebookControlled extends SingleWorkspaceUnit {
  @Test
  @DisplayName("list and describe reflect creating and deleting a controlled notebook")
  void listDescribeReflectCreateDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcp-notebook --name=$name`
    String name = "listDescribeReflectCreateDelete";
    UFGcpNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class,
            "resource",
            "create",
            "gcp-notebook",
            "--name=" + name,
            "--metadata=foo=bar");

    // check that the name and notebook name match
    assertEquals(name, createdNotebook.name, "create output matches name");

    // gcp notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");

    // check that the notebook is in the list
    UFGcpNotebook matchedResource = listOneNotebookResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");

    // `terra resource describe --name=$name --format=json`
    UFGcpNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class, "resource", "describe", "--name=" + name);

    // check that the name matches and the instance id is populated
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertNotNull(describeResource.instanceName, "describe resource output includes instance name");

    // gcp notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");

    // `terra notebook delete --name=$name`
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
      List<UFGcpNotebook> listedNotebooks = listNotebookResourcesWithName(name);
      assertThat(
          "deleted notebook no longer appears in the resources list",
          listedNotebooks,
          Matchers.empty());
    }
  }

  @Test
  @DisplayName("resolve and check-access for a controlled notebook")
  void resolveAndCheckAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcp-notebook --name=$name`
    String name = "resolveAndCheckAccess";
    UFGcpNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class, "resource", "create", "gcp-notebook", "--name=" + name);

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(createdNotebook.instanceName, resolved, "resolve returns the instance name");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "check-access error is because gcp notebooks are controlled resources",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));
  }

  @Test
  @DisplayName("override the default location and instance id")
  void overrideLocationAndInstanceId() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcp-notebook --name=$name
    // --cloning=$cloning --description=$description
    // --location=$location --instance-id=$instanceId`
    String name = "overrideLocationAndInstanceId";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
    String description = "\"override default location and instance id\"";
    String location = "us-central1-b";
    String instanceId = "a" + UUID.randomUUID().toString(); // instance id must start with a letter
    UFGcpNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class,
            "resource",
            "create",
            "gcp-notebook",
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

    // gcp notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");

    // `terra resource describe --name=$name --format=json`
    UFGcpNotebook describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(location, describeResource.location, "describe resource output matches location");
    assertEquals(
        instanceId, describeResource.instanceId, "describe resource output matches instance id");

    // gcp notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, describeResource.accessScope, "describe output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");

    String newName = "NewOverrideLocationAndInstanceId";
    String newDescription = "\"new override default location and instance id\"";
    String newMetadata = "NewMetadata=metadata";
    UFGcpNotebook updatedNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class,
            "resource",
            "update",
            "gcp-notebook",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription,
            "--new-metadata=" + newMetadata);

    // check that the properties match
    assertEquals(newName, updatedNotebook.name, "create output matches name");
    assertEquals(newDescription, updatedNotebook.description, "create output matches description");
  }

  @Test // NOTE: This test takes ~10 minutes to run.
  @DisplayName("start, stop a notebook and poll until they complete")
  void startStop() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcp-notebook --name=$name`
    String name = "startStop";
    TestCommand.runCommandExpectSuccess("resource", "create", "gcp-notebook", "--name=" + name);
    pollDescribeForNotebookState(name, "ACTIVE");

    // `terra notebook stop --name=$name`
    TestCommand.Result cmd = TestCommand.runCommand("notebook", "stop", "--name=" + name);
    boolean badState409 =
        cmd.exitCode == 1 && cmd.stdErr.contains("409: unable to queue the operation");
    assertTrue(
        cmd.exitCode == 0 || badState409,
        "stop either succeeds or fails with a 409 bad state error");
    if (!badState409) {
      assertNotebookState(name, "STOPPED");
    }

    // `terra notebook start --name=$name`
    cmd = TestCommand.runCommand("notebook", "start", "--name=" + name);
    badState409 = cmd.exitCode == 1 && cmd.stdErr.contains("409: unable to queue the operation");
    assertTrue(
        cmd.exitCode == 0 || badState409,
        "start either succeeds or fails with a 409 bad state error");
    if (!badState409) {
      assertNotebookState(name, "ACTIVE");
    }
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
        2 * 20, // up to 20 minutes
        Duration.ofSeconds(30)); // every 30 seconds

    assertNotebookState(resourceName, notebookState, workspaceUserFacingId);
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
  private static void assertNotebookState(
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
  static UFGcpNotebook listOneNotebookResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneNotebookResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  static UFGcpNotebook listOneNotebookResourceWithName(
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
  static List<UFGcpNotebook> listNotebookResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listNotebookResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Filters on the specified workspace id; Uses the current workspace if null.
   */
  static List<UFGcpNotebook> listNotebookResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=GCP_NOTEBOOK --format=json`
    List<UFGcpNotebook> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=GCP_NOTEBOOK")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=GCP_NOTEBOOK",
                "--workspace=" + workspaceUserFacingId);

    // find the matching notebook in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
