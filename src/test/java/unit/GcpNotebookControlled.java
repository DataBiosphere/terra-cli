package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.GcpNotebookUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled GCP notebooks. */
@Tag("unit-gcp")
public class GcpNotebookControlled extends SingleWorkspaceUnitGcp {
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
    assertEquals("bar", createdNotebook.metadata.get("foo"), "create output matches metadata");

    // gcp notebooks are always private
    assertEquals(
        AccessScope.PRIVATE_ACCESS, createdNotebook.accessScope, "create output matches access");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdNotebook.privateUserName.toLowerCase(),
        "create output matches private user name");

    // check that the notebook is in the list
    UFGcpNotebook matchedResource = GcpNotebookUtils.listOneNotebookResourceWithName(name);
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
      List<UFGcpNotebook> listedNotebooks = GcpNotebookUtils.listNotebookResourcesWithName(name);
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
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertEquals(
        createdNotebook.instanceName, resolved.get(name), "resolve returns the instance name");

    // `terra resource check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "check-access error because gcp notebooks are controlled resources",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));
  }

  @Test
  @DisplayName("override the default location and instance id of a controlled notebook")
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
    String instanceId = "a" + UUID.randomUUID(); // instance id must start with a letter
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

    // new key-value pair will be appended, existing key-value pair will be updated.
    String newName = "NewOverrideLocationAndInstanceId";
    String newDescription = "\"new override default location and instance id\"";
    String newKey1 = "NewMetadata1";
    String newKey2 = "NewMetadata2";
    String newValue1 = "metadata1";
    String newValue2 = "metadata2";
    String newEntry1 = newKey1 + "=" + newValue1;
    String newEntry2 = newKey2 + "=" + newValue2;
    UFGcpNotebook updatedNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class,
            "resource",
            "update",
            "gcp-notebook",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription,
            "--new-metadata=" + newEntry1 + "," + newEntry2);

    // check that the properties match
    // the metadata supports multiple entries, we can't assert on the metadata because it's not
    // stored in or accessible via Workspace Manager.
    assertEquals(newName, updatedNotebook.name, "create output matches name");
    assertEquals(newDescription, updatedNotebook.description, "create output matches description");
    assertEquals(
        newValue1,
        updatedNotebook.metadata.get(newKey1),
        "create output matches metadata" + newKey1 + ": " + newValue1);
    assertEquals(
        newValue2,
        updatedNotebook.metadata.get(newKey2),
        "create output matches metadata" + newKey2 + ": " + newValue2);
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
    GcpNotebookUtils.pollDescribeForNotebookState(name, "ACTIVE");

    // Poll until the test user can get the notebook IAM bindings directly to confirm cloud
    // permissions have
    // synced. This works because we give "notebooks.instances.getIamPolicy" to notebook editors.
    // The UFGcpNotebook object is not fully populated at creation time, so we need an additional
    // `describe` call here.
    UFGcpNotebook createdNotebook =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcpNotebook.class, "resource", "describe", "--name=" + name);
    CrlUtils.callGcpWithPermissionExceptionRetries(
        () ->
            CrlUtils.createNotebooksCow(workspaceCreator.getCredentialsWithCloudPlatformScope())
                .instances()
                .getIamPolicy(createdNotebook.instanceName)
                .execute(),
        Objects::nonNull);

    // `terra notebook stop --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "stop", "--name=" + name);
    GcpNotebookUtils.assertNotebookState(name, "STOPPED");

    // `terra notebook start --name=$name`
    TestCommand.runCommandExpectSuccessWithRetries("notebook", "start", "--name=" + name);
    GcpNotebookUtils.assertNotebookState(name, "ACTIVE");
  }
}
