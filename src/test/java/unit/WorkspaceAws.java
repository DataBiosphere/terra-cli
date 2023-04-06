package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.workspace.model.CloudPlatform;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands specific to CloudPlatform.AWS. */
@Tag("unit-aws")
public class WorkspaceAws extends ClearContextUnit {
  @BeforeAll
  protected void setupOnce() throws Exception {
    setCloudPlatform(CloudPlatform.AWS);
    super.setupOnce();
  }

  @Test
  @DisplayName("status and describe list reflect workspace create")
  void statusDescribeListReflectCreate() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // check the created workspace has an id and a google project
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(createdWorkspace.awsAccountId, "create workspace returned a aws account id");
    assertNotNull(createdWorkspace.awsTenantAlias, "create workspace returned a aws tenant alias");
    assertThat(
        "workspace email matches test user",
        createdWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));

    // check the created workspace has cloud platform set
    assertThat(
        "workspace cloudPlatform matches AWS",
        CloudPlatform.AWS,
        equalTo(createdWorkspace.cloudPlatform));

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the new workspace
    assertThat(
        "workspace server matches current server",
        createdWorkspace.serverName,
        equalToIgnoringCase(status.server.name));
    assertEquals(createdWorkspace.id, status.workspace.id, "workspace id matches current status");
    assertEquals(
        createdWorkspace.awsAccountId,
        status.workspace.awsAccountId,
        "workspace aws account id matches current status");
    assertEquals(
        createdWorkspace.awsTenantAlias,
        status.workspace.awsTenantAlias,
        "workspace aws tenant alias matches current status");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertEquals(
        createdWorkspace.id, describedWorkspace.id, "workspace id matches that in describe");
    assertEquals(
        createdWorkspace.awsAccountId,
        describedWorkspace.awsAccountId,
        "workspace aws id number matches that in describe");
    assertEquals(
        createdWorkspace.awsTenantAlias,
        describedWorkspace.awsTenantAlias,
        "workspace aws tenant alias matches that in describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertEquals(
        createdWorkspace.id, matchingWorkspaces.get(0).id, "workspace id matches that in list");
    assertEquals(
        createdWorkspace.awsAccountId,
        matchingWorkspaces.get(0).awsAccountId,
        "workspace aws id number matches that in list");
    assertEquals(
        createdWorkspace.awsTenantAlias,
        matchingWorkspaces.get(0).awsTenantAlias,
        "workspace aws tenant alias matches that in list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
