package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestCommand.Result;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands. */
@Tag("unit")
public class Workspace extends ClearContextUnit {
  @Test
  @DisplayName("status, describe, workspace list reflect workspace create")
  void statusDescribeListReflectCreate() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(testUser);

    // check the created workspace has an id and a google project
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(createdWorkspace.googleProjectId, "create workspace created a gcp project");
    assertThat(
        "workspace email matches test user",
        createdWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the new workspace
    assertThat(
        "workspace server matches current server",
        createdWorkspace.serverName,
        equalToIgnoringCase(status.server.name));
    assertEquals(createdWorkspace.id, status.workspace.id, "workspace id matches current status");
    assertEquals(
        createdWorkspace.googleProjectId,
        status.workspace.googleProjectId,
        "workspace gcp project matches current status");

    // `terra workspace describe --format=json`
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertEquals(
        createdWorkspace.id, describeWorkspace.id, "workspace id matches that in describe");
    assertEquals(
        createdWorkspace.googleProjectId,
        describeWorkspace.googleProjectId,
        "workspace gcp project matches that in describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces = listWorkspacesWithId(createdWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertEquals(
        createdWorkspace.id, matchingWorkspaces.get(0).id, "workspace id matches that in list");
    assertEquals(
        createdWorkspace.googleProjectId,
        matchingWorkspaces.get(0).googleProjectId,
        "workspace gcp project matches that in list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("status, describe, workspace list reflect workspace delete")
  void statusDescribeListReflectDelete() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(testUser);

    // `terra workspace delete --format=json`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the deleted workspace
    assertNull(status.workspace, "status has no workspace after delete");

    // `terra workspace describe --format=json`
    TestCommand.runCommandExpectExitCode(1, "workspace", "describe");

    // check the deleted workspace is not included in the list
    List<UFWorkspaceLight> matchingWorkspaces = listWorkspacesWithId(createdWorkspace.id);
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace is not included in list");
  }

  @Test
  @DisplayName("status, describe, workspace list reflect workspace update")
  void statusDescribeListReflectUpdate() throws IOException {
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

    TestCommand.runCommandExpectSuccess(
        "workspace", "set-property", "--properties=key=valueUpdate,key1=value1");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the update
    assertEquals(newId, status.workspace.id, "status matches updated workspace id");
    assertEquals(newName, status.workspace.name, "status matches updated workspace name");
    assertEquals(
        newDescription,
        status.workspace.description,
        "status matches updated workspace description");

    assertEquals("valueUpdate", status.workspace.properties.get("key"));
    assertEquals("value1", status.workspace.properties.get("key1"));
    assertEquals(
        2, status.workspace.properties.size(), "Multiple property entries updated successful.");

    // `terra workspace describe --format=json`
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the update
    assertEquals(newName, describeWorkspace.name, "describe matches updated workspace name");
    assertEquals(
        newDescription,
        describeWorkspace.description,
        "describe matches updated workspace description");

    // check the workspace list reflects the update
    List<UFWorkspaceLight> matchingWorkspaces = listWorkspacesWithId(newId);
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
  @DisplayName("status, describe reflect workspace set")
  void statusDescribeReflectsSet() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace1 = WorkspaceUtils.createWorkspace(testUser);
    UFWorkspace createdWorkspace2 = WorkspaceUtils.createWorkspace(testUser);

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
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createdWorkspace1.id, describeWorkspace.id, "describe matches set workspace id (1)");

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
    describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createdWorkspace2.id, describeWorkspace.id, "describe matches set workspace id (2)");

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

  @Test
  @DisplayName("workspace describe reflects the number of resources")
  void describeReflectsNumResources() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(testUser);
    assertEquals(0, createdWorkspace.numResources, "new workspace has 0 resources");

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName`
    String bucketResourceName = "describeReflectsNumResourcesGCS";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "gcs-bucket",
        "--name=" + bucketResourceName,
        "--bucket-name=" + bucketName);

    // `terra workspace describe`
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        1, describeWorkspace.numResources, "worksapce has 1 resource after creating bucket");

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId`
    String datasetResourceName = "describeReflectsNumResourcesBQ";
    String datasetId = "bq1";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "bq-dataset",
        "--name=" + datasetResourceName,
        "--dataset-id=" + datasetId);

    // `terra workspace describe`
    Result describeResult2 = TestCommand.runCommand("workspace", "describe");
    assertEquals(0, describeResult2.exitCode, "Describe was successful.");
    assertThat(
        "workspace has 2 resources after creating dataset",
        describeResult2.stdOut,
        containsString("# Resources:       2"));
    assertThat(
        "No error message is displayed on second describe.",
        describeResult2.stdErr,
        is(emptyString()));

    UFWorkspace describeWorkspace3 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        2, describeWorkspace3.numResources, "worksapce has 2 resources after creating dataset");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  /**
   * Helper method to call `terra workspace list` and filter the results on the specified workspace
   * id. Use a high limit to ensure that leaked workspaces in the list don't cause the one we care
   * about to page out.
   */
  static List<UFWorkspaceLight> listWorkspacesWithId(String userFacingId)
      throws JsonProcessingException {
    // `terra workspace list --format=json --limit=500`
    List<UFWorkspaceLight> listWorkspaces =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--limit=500");

    return listWorkspaces.stream()
        .filter(workspace -> workspace.id.equals(userFacingId))
        .collect(Collectors.toList());
  }
}
