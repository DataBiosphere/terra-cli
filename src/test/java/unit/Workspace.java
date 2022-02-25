package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
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

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // check the created workspace has an id and a google project
    assertNotNull(createWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(createWorkspace.googleProjectId, "create workspace created a gcp project");
    assertThat(
        "workspace email matches test user",
        createWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

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

    // `terra workspace describe --format=json`
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertEquals(createWorkspace.id, describeWorkspace.id, "workspace id matches that in describe");
    assertEquals(
        createWorkspace.googleProjectId,
        describeWorkspace.googleProjectId,
        "workspace gcp project matches that in describe");

    // check the new workspace is included in the list
    List<UFWorkspace> matchingWorkspaces = listWorkspacesWithId(createWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertEquals(
        createWorkspace.id, matchingWorkspaces.get(0).id, "workspace id matches that in list");
    assertEquals(
        createWorkspace.googleProjectId,
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

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // `terra workspace delete --format=json`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the deleted workspace
    assertNull(status.workspace, "status has no workspace after delete");

    // `terra workspace describe --format=json`
    TestCommand.runCommandExpectExitCode(1, "workspace", "describe");

    // check the deleted workspace is not included in the list
    List<UFWorkspace> matchingWorkspaces = listWorkspacesWithId(createWorkspace.id);
    assertEquals(0, matchingWorkspaces.size(), "deleted workspace is not included in list");
  }

  @Test
  @DisplayName("status, describe, workspace list reflect workspace update")
  void statusDescribeListReflectUpdate() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create --format=json --name=$name --description=$description`
    String name = "statusDescribeListReflectUpdate";
    String description = "status list reflect update";
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class,
            "workspace",
            "create",
            "--name=" + name,
            "--description=" + description);

    // check the created workspace name and description are set
    assertNotNull(createWorkspace.name, "create workspace name is defined");
    assertNotNull(createWorkspace.description, "create workspace description is defined");

    // `terra workspace create --format=json --name=$newName --description=$newDescription`
    String newName = "NEW_statusDescribeListReflectUpdate";
    String newDescription = "NEW status describe list reflect update";
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "update",
        "--format=json",
        "--name=" + newName,
        "--description=" + newDescription);

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the update
    assertEquals(newName, status.workspace.name, "status matches updated workspace name");
    assertEquals(
        newDescription,
        status.workspace.description,
        "status matches updated workspace description");

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
    List<UFWorkspace> matchingWorkspaces = listWorkspacesWithId(createWorkspace.id);
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

    // `terra workspace create --format=json` (workspace 1)
    UFWorkspace createWorkspace1 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // `terra workspace create --format=json` (workspace 2)
    UFWorkspace createWorkspace2 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // set current workspace = workspace 1
    UFWorkspace setWorkspace1 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createWorkspace1.id);
    assertEquals(createWorkspace1.id, setWorkspace1.id, "set returned the expected workspace (1)");

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the workspace set
    assertEquals(createWorkspace1.id, status.workspace.id, "status matches set workspace id (1)");

    // `terra workspace describe --format=json`
    UFWorkspace describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createWorkspace1.id, describeWorkspace.id, "describe matches set workspace id (1)");

    // set current workspace = workspace 2
    UFWorkspace setWorkspace2 =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + createWorkspace2.id);
    assertEquals(createWorkspace2.id, setWorkspace2.id, "set returned the expected workspace (2)");

    // `terra status --format=json`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the workspace set
    assertEquals(createWorkspace2.id, status.workspace.id, "status matches set workspace id (2)");

    // `terra workspace describe --format=json`
    describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the workspace describe reflects the workspace set
    assertEquals(
        createWorkspace2.id, describeWorkspace.id, "describe matches set workspace id (2)");

    // `terra workspace delete` (workspace 2)
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");

    // `terra workspace set` (workspace 1)
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace1.id);

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
    // `terra workspace create`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "workspace", "create", "--name=" + workspaceName);
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
  @DisplayName("workspace describe reflects the number of resources")
  void describeReflectsNumResources() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    // `terra workspace create`
    UFWorkspace createdWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
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
    describeWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        2, describeWorkspace.numResources, "worksapce has 2 resources after creating dataset");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  /**
   * Helper method to call `terra workspace list` and filter the results on the specified workspace
   * id. Use a high limit to ensure that leaked workspaces in the list don't cause the one we care
   * about to page out.
   */
  static List<UFWorkspace> listWorkspacesWithId(UUID workspaceId) throws JsonProcessingException {
    // `terra workspace list --format=json --limit=500`
    List<UFWorkspace> listWorkspaces =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--limit=500");

    return listWorkspaces.stream()
        .filter(workspace -> workspace.id.equals(workspaceId))
        .collect(Collectors.toList());
  }
}
