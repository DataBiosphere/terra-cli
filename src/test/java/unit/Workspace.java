package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.cli.service.UserManagerService;
import bio.terra.cli.service.WorkspaceManagerService;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands. */
@Tag("unit")
public class Workspace extends ClearContextUnit {
  @Test
  @DisplayName("workspace create uses spend profile stored in user manager")
  void create_spendProfileFromUserManager() throws IOException, InterruptedException {
    if (!Context.getServer().getName().equals("verily-devel")) {
      return;
    }
    var altSpendProfile = "wm-alt-spend-profile";
    assumeTrue(Context.getServer().getUserManagerUri() != null);

    // Use the spend owner account
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // Set the default in the user manager
    UserManagerService.fromContext().setDefaultSpendProfile(/*email=*/ null, altSpendProfile);

    // Create the workspace using the spend profile
    WorkspaceUtils.createWorkspace(spendProfileOwner, Optional.empty());

    var workspaceDescription =
        WorkspaceManagerService.fromContext().getWorkspace(Context.requireWorkspace().getUuid());
    assertEquals(altSpendProfile, workspaceDescription.getSpendProfile());

    // Remove the default in the user manager
    UserManagerService.fromContext()
        .setDefaultSpendProfile(/*email=*/ null, /*spendProfile=*/ null);
  }

  @Test
  @DisplayName("workspace clone uses spend profile stored in user manager")
  void clone_spendProfileFromUserManager() throws IOException, InterruptedException {
    if (!Context.getServer().getName().equals("verily-devel")) {
      return;
    }
    var altSpendProfile = "wm-alt-spend-profile";
    assumeTrue(Context.getServer().getUserManagerUri() != null);

    // Use the spend owner account
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // Create a workspace without a chosen default
    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(spendProfileOwner, Optional.empty());

    // Set the default in the user manager
    UserManagerService.fromContext().setDefaultSpendProfile(/*email=*/ null, altSpendProfile);

    // Clone the workspace using the spend profile
    TestCommand.runCommandExpectSuccess(
        "workspace", "clone", "--new-id=" + createdWorkspace.id + "-clone");

    var workspaceDescription =
        WorkspaceManagerService.fromContext()
            .getWorkspaceByUserFacingId(createdWorkspace.id + "-clone");
    assertEquals(altSpendProfile, workspaceDescription.getSpendProfile());
    // Remove the default in the user manager
    UserManagerService.fromContext()
        .setDefaultSpendProfile(/*email=*/ null, /*spendProfile=*/ null);
  }

  @Test
  @DisplayName("status, describe, workspace list reflect workspace delete")
  void statusDescribeListReflectDelete() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // `terra workspace delete --format=json`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the deleted workspace
    assertNull(status.workspace, "status has no workspace after delete");

    // `terra workspace describe --format=json`
    TestCommand.runCommandExpectExitCode(1, "workspace", "describe");

    // check the deleted workspace is not included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace is not included in list");
  }

  @Test
  @DisplayName("delete property")
  void deleteProperty() throws IOException, InterruptedException {
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();
    String initialProperties = "key=value,key1=value1,foo=bar";

    // create a workspace with 3 properties
    WorkspaceUtils.createWorkspace(testUser, "propertyDeleteTest", "", initialProperties);

    // call `terra workspace delete-property` for 2 properties
    TestCommand.runCommandExpectSuccess("workspace", "delete-property", "--keys=key,key1");
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check workspace only has 1 property
    assertEquals("bar", describedWorkspace.properties.get("foo"));
    assertEquals(1, describedWorkspace.properties.size());

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("status, describe, workspace list reflect workspace update")
  void statusDescribeListReflectUpdate() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    String name = "statusDescribeListReflectUpdate";
    String description = "status list reflect update";
    String key = "key";
    String value = "value";
    String property = key + "=" + value;

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, name, description, property);

    // check the created workspace name, description, property are set
    assertEquals(name, createdWorkspace.name);
    assertEquals(description, createdWorkspace.description);
    assertTrue(createdWorkspace.properties.containsKey(key));
    assertEquals(value, createdWorkspace.properties.get(key));

    // `terra workspace update --format=json --new-id=$newId --new-name=$newName
    // --new-description=$newDescription`
    String newId = "new-" + createdWorkspace.id;
    String newName = "NEW_statusDescribeListReflectUpdate";
    String newDescription = "NEW status describe list reflect update";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--format=json",
        "--new-id=" + newId,
        "--new-name=" + newName,
        "--new-description=" + newDescription);

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the update
    assertEquals(newId, status.workspace.id, "status matches updated workspace id");
    assertEquals(newName, status.workspace.name, "status matches updated workspace name");
    assertEquals(
        newDescription,
        status.workspace.description,
        "status matches updated workspace description");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the update
    assertEquals(newName, describedWorkspace.name, "describe matches updated workspace name");
    assertEquals(
        newDescription,
        describedWorkspace.description,
        "describe matches updated workspace description");

    // check the workspace list reflects the update
    List<UFWorkspaceLight> matchingWorkspaces = WorkspaceUtils.listWorkspacesWithId(newId);
    assertEquals(
        1, matchingWorkspaces.size(), "updated workspace is included exactly once in list");
    assertEquals(
        newName, matchingWorkspaces.get(0).name, "updated workspace name matches that in list");
    assertEquals(
        newDescription,
        matchingWorkspaces.get(0).description,
        "updated workspace description matches that in list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("update properties in workspace")
  void updateProperty() throws IOException, InterruptedException {
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();
    String initialProperties = "key=value,key1=value1";

    // create a workspace with 2 properties key=value, key1=value1
    WorkspaceUtils.createWorkspace(testUser, "propertyUpdateTest", "", initialProperties);

    // call `terra workspace set-property` for 2 properties, key=valueUpdate, foo=bar
    TestCommand.runCommandExpectSuccess(
        "workspace", "set-property", "--properties=key=valueUpdate,foo=bar");
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // assert 3 properties are correct
    assertEquals("valueUpdate", describedWorkspace.properties.get("key"));
    assertEquals("value1", describedWorkspace.properties.get("key1"));
    assertEquals("bar", describedWorkspace.properties.get("foo"));
    assertEquals(
        3, describedWorkspace.properties.size(), "Multiple property entries updated successful.");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("status, describe reflect workspace set")
  void statusDescribeReflectsSet() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace1 =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));
    UFWorkspace createdWorkspace2 =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // set current workspace = workspace 1
    UFWorkspace setWorkspace1 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createdWorkspace1.id);
    assertEquals(createdWorkspace1.id, setWorkspace1.id, "set returned the expected workspace (1)");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the workspace set
    assertEquals(createdWorkspace1.id, status.workspace.id, "status matches set workspace id (1)");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createdWorkspace1.id, describedWorkspace.id, "describe matches set workspace id (1)");

    // set current workspace = workspace 2
    UFWorkspace setWorkspace2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createdWorkspace2.id);
    assertEquals(createdWorkspace2.id, setWorkspace2.id, "set returned the expected workspace (2)");

    // `terra status --format=json`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the workspace set
    assertEquals(createdWorkspace2.id, status.workspace.id, "status matches set workspace id (2)");

    // `terra workspace describe --format=json`
    describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createdWorkspace2.id, describedWorkspace.id, "describe matches set workspace id (2)");

    // `terra workspace delete` (workspace 2)
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra workspace set` (workspace 1)
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createdWorkspace1.id);

    // `terra workspace delete` (workspace 1)
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("workspace create fails without spend profile access")
  void createFailsWithoutSpendAccess() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithoutSpendAccess();
    testUser.login();
    final String workspaceName = "bad-profile-6789";
    // `terra workspace create --id=<user-facing-id>`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "workspace", "create", "--id=" + WorkspaceUtils.createUserFacingId());
    assertThat(
        "error message includes spend profile unauthorized",
        stdErr,
        CoreMatchers.containsString(
            "Accessing the spend profile failed. Ask an administrator to grant you access."));

    // workspace was deleted
    List<UFWorkspace> listWorkspaces =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--limit=100");
    assertFalse(listWorkspaces.stream().anyMatch(w -> workspaceName.equals(w.name)));
  }

  @Test
  @DisplayName("workspace create fails without userFacing id")
  void createFailsWithoutUserFacingId() throws IOException {
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "workspace", "create");
    assertThat(
        "error message indicate user must set ID",
        stdErr,
        CoreMatchers.containsString("Missing required option: '--id=<id>'"));
  }
}
