package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands. */
@Tag("unit")
public class Workspace extends ClearContextUnit {
  @Test
  @DisplayName("status, workspace list reflect workspace create")
  void statusListReflectCreate() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--format=json");

    // check the created workspace has an id and a google project
    assertNotNull(createWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(createWorkspace.googleProjectId, "create workspace created a gcp project");
    assertThat(
        "workspace email matches test user",
        createWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));

    // `terra status --format=json`
    UFStatus status =
        TestCommand.runCommandExpectSuccess(UFStatus.class, "status", "--format=json");

    // check the current status reflects the new workspace
    assertThat(
        "workspace server matches current server",
        createWorkspace.serverName,
        equalToIgnoringCase(status.server.name));
    assertEquals(createWorkspace.id, status.workspace.id, "workspace id matches current status");
    assertEquals(
        createWorkspace.googleProjectId,
        status.workspace.googleProjectId,
        "workspace gcp project matches current status");

    // `terra workspace list --format=json`
    List<UFWorkspace> listWorkspaces =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--format=json");

    // check the new workspace is included in the list
    List<UFWorkspace> matchingWorkspaces =
        listWorkspaces.stream()
            .filter(workspace -> workspace.id.equals(createWorkspace.id))
            .collect(Collectors.toList());
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertEquals(createWorkspace.id, listWorkspaces.get(0).id, "workspace id matches that in list");
    assertEquals(
        createWorkspace.googleProjectId,
        listWorkspaces.get(0).googleProjectId,
        "workspace gcp project matches that in list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete");
  }

  @Test
  @DisplayName("status, workspace list reflect workspace delete")
  void statusListReflectDelete() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--format=json");

    // `terra workspace delete --format=json`
    UFWorkspace deleteWorkspace =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "delete", "--format=json");

    // check the deleted workspace matches the created workspace
    assertEquals(createWorkspace.id, deleteWorkspace.id, "deleted workspace id matches created");
    assertEquals(
        createWorkspace.googleProjectId,
        deleteWorkspace.googleProjectId,
        "deleted workspace gcp project matches created");

    // `terra status --format=json`
    UFStatus status =
        TestCommand.runCommandExpectSuccess(UFStatus.class, "status", "--format=json");

    // check the current status reflects the deleted workspace
    assertNull(status.workspace, "status has no workspace after delete");

    // `terra workspace list --format=json`
    List<UFWorkspace> listWorkspaces =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--format=json");

    // check the deleted workspace is not included in the list
    List<UFWorkspace> matchingWorkspaces =
        listWorkspaces.stream()
            .filter(workspace -> workspace.id.equals(createWorkspace.id))
            .collect(Collectors.toList());
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace is not included in list");
  }

  @Test
  @DisplayName("status, workspace list reflect workspace update")
  void statusListReflectUpdate() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create --format=json --name=$name --description=$description`
    String name = "statusListReflectUpdate";
    String description = "status list reflect update";
    UFWorkspace createWorkspace =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class,
            "workspace",
            "create",
            "--format=json",
            "--name=" + name,
            "--description=" + description);

    // check the created workspace name and description are set
    assertNotNull(createWorkspace.name, "create workspace name is defined");
    assertNotNull(createWorkspace.description, "create workspace description is defined");

    // `terra workspace create --format=json --name=$newName --description=$newDescription`
    String newName = "NEW_statusListReflectUpdate";
    String newDescription = "NEW status list reflect update";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--format=json",
        "--name=" + newName,
        "--description=" + newDescription);

    // `terra status --format=json`
    UFStatus status =
        TestCommand.runCommandExpectSuccess(UFStatus.class, "status", "--format=json");

    // check the current status reflects the update
    assertEquals(newName, status.workspace.name, "status matches updated workspace name");
    assertEquals(
        newDescription,
        status.workspace.description,
        "status matches updated workspace description");

    // `terra workspace list --format=json`
    List<UFWorkspace> listWorkspaces =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--format=json");

    // check the workspace list reflects the update
    List<UFWorkspace> matchingWorkspaces =
        listWorkspaces.stream()
            .filter(workspace -> workspace.id.equals(createWorkspace.id))
            .collect(Collectors.toList());
    assertEquals(
        1, matchingWorkspaces.size(), "updated workspace is included exactly once in list");
    assertEquals(
        newName, matchingWorkspaces.get(0).name, "updated workspace name matches that in list");
    assertEquals(
        newDescription,
        matchingWorkspaces.get(0).description,
        "updated workspace description matches that in list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete");
  }

  @Test
  @DisplayName("status reflects workspace set")
  void statusReflectsSet() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create --format=json` (workspace 1)
    UFWorkspace createWorkspace1 =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--format=json");

    // `terra workspace create --format=json` (workspace 2)
    UFWorkspace createWorkspace2 =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--format=json");

    // set current workspace = workspace 1
    UFWorkspace setWorkspace1 =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createWorkspace1.id, "--format=json");
    assertEquals(createWorkspace1.id, setWorkspace1.id, "set returned the expected workspace (1)");

    // `terra status --format=json`
    UFStatus status =
        TestCommand.runCommandExpectSuccess(UFStatus.class, "status", "--format=json");

    // check the current status reflects the workspace set
    assertEquals(createWorkspace1.id, status.workspace.id, "status matches set workspace id (1)");

    // set current workspace = workspace 2
    UFWorkspace setWorkspace2 =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createWorkspace2.id, "--format=json");
    assertEquals(createWorkspace2.id, setWorkspace2.id, "set returned the expected workspace (2)");

    // `terra status --format=json`
    status = TestCommand.runCommandExpectSuccess(UFStatus.class, "status", "--format=json");

    // check the current status reflects the workspace set
    assertEquals(createWorkspace2.id, status.workspace.id, "status matches set workspace id (2)");

    // `terra workspace delete` (workspace 2)
    TestCommand.runCommandExpectSuccess("workspace", "delete");

    // `terra workspace set` (workspace 1)
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace1.id);

    // `terra workspace delete` (workspace 1)
    TestCommand.runCommandExpectSuccess("workspace", "delete");
  }

  @Test
  @DisplayName("workspace create fails without spend profile access")
  void createFailsWithoutSpendAccess() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUserWithoutSpendAccess();
    testUser.login();

    // `terra workspace create`
    TestCommand.Result cmd = TestCommand.runCommand("workspace", "create");
    assertEquals(2, cmd.exitCode, "exit code = system exception");
    assertThat(
        "error message includes spend profile unauthorized",
        cmd.stdErr,
        CoreMatchers.containsString("User is unauthorized to link spend profile"));
  }
}
