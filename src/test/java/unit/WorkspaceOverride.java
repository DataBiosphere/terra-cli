package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.WorkspaceUser.expectListedUserWithRoles;
import static unit.WorkspaceUser.workspaceListUsersWithEmail;

import bio.terra.cli.businessobject.WorkspaceUser;
import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.cli.serialization.userfacing.UFWorkspaceUser;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `--workspace` option to override the current workspace just for this command. */
@Tag("unit")
public class WorkspaceOverride extends ClearContextUnit {
  private static UFWorkspace workspace1;
  private static UFWorkspace workspace2;

  /**
   * Create two workspaces for tests to use, so we can switch between them with the override option.
   */
  @Override
  @BeforeAll
  protected void setupOnce() throws Exception {
    super.setupOnce();

    workspace1 = WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));
    workspace2 = WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));
  }

  /** Delete the two workspaces. */
  @AfterAll
  protected void cleanupOnce() throws IOException, GeneralSecurityException {
    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace2.id);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("workspace user commands respect workspace override")
  void workspaceUser() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUser testUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace add-user --email=$email --role=READER --workspace=$id2`
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "add-user",
        "--email=" + testUser.email,
        "--role=READER",
        "--workspace=" + workspace2.id);

    // check that the user exists in the list output for workspace 2, but not workspace 1

    // `terra workspace list-users --email=$email --role=READER --workspace=$id2`
    expectListedUserWithRoles(testUser.email, workspace2.id, WorkspaceUser.Role.READER);

    // `terra workspace list-users --email=$email --role=READER`
    Optional<UFWorkspaceUser> workspaceUser = workspaceListUsersWithEmail(testUser.email);
    assertTrue(workspaceUser.isEmpty(), "test user is not in users list for workspace 1");

    // `terra workspace remove-user --email=$email --role=READER --workspace=$id2`
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "remove-user",
        "--email=" + testUser.email,
        "--role=READER",
        "--workspace=" + workspace2.id);

    // `terra workspace list-users --email=$email --role=READER --workspace=$id2`
    // check that the user no longer exists in the list output for workspace 2
    workspaceUser = workspaceListUsersWithEmail(testUser.email, workspace2.id);
    assertTrue(workspaceUser.isEmpty(), "test user is no longer in users list for workspace 2");
  }

  @Test
  @DisplayName("workspace commands respect workspace override")
  void workspace() throws IOException, InterruptedException {
    workspaceCreator.login();

    UFWorkspace workspace3 =
        WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));

    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace1.id);

    // `terra workspace update --name=$newName --new-description=$newDescription --workspace=$id3`
    String newName = "workspace3_name_NEW";
    String newDescription = "workspace3 description NEW";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--new-name=" + newName,
        "--new-description=" + newDescription,
        "--workspace=" + workspace3.id);

    // check that the workspace 3 description has been updated, and the workspace 1 has not

    // `terra workspace describe --workspace=$id3`
    UFWorkspace workspaceDescribe =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "describe", "--workspace=" + workspace3.id);
    assertEquals(newName, workspaceDescribe.name, "workspace 3 name matches update");
    assertEquals(
        newDescription, workspaceDescribe.description, "workspace 3 description matches update");

    // `terra workspace describe`
    workspaceDescribe =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(workspace1.name, workspaceDescribe.name, "workspace 1 name matches create");
    assertEquals(
        workspace1.description,
        workspaceDescribe.description,
        "workspace 1 description matches create");

    // check the workspace 3 has been deleted, and workspace 1 has not

    // `terra workspace delete --workspace=$id3`
    TestCommand.runCommandExpectSuccess(
        "workspace", "delete", "--workspace=" + workspace3.id, "--quiet");

    // `terra workspace list`
    List<UFWorkspaceLight> matchingWorkspaces = WorkspaceUtils.listWorkspacesWithId(workspace3.id);
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace 3 is not included in list");

    // `terra workspace list`
    matchingWorkspaces = WorkspaceUtils.listWorkspacesWithId(workspace1.id);
    assertEquals(1, matchingWorkspaces.size(), "workspace 1 is still included in list");
  }

  @Test
  @DisplayName("workspace commands ignore workspace override when it matches current workspace")
  void matchingCurrentWorkspace() throws IOException, InterruptedException {
    workspaceCreator.login();

    UFWorkspace workspace3 =
        WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));

    // `terra workspace set --id=$id3`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspace3.id);

    // `terra workspace update --name=$newName --new-description=$newDescription --workspace=$id3`
    String newName = "workspace3_name_NEW";
    String newDescription = "workspace3 description NEW";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--new-name=" + newName,
        "--new-description=" + newDescription,
        "--workspace=" + workspace3.id);

    // Check that current workspace status is updated, despite the --workspace flag.
    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(newName, status.workspace.name);
    assertEquals(newDescription, status.workspace.description);

    // `terra workspace delete --workspace=$id3`
    TestCommand.runCommandExpectSuccess(
        "workspace", "delete", "--workspace=" + workspace3.id, "--quiet");
    // Confirm current workspace status was cleared, despite the --workspace flag.
    UFStatus clearedStatus = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertNull(clearedStatus.workspace);
  }
}
