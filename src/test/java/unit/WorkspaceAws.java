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
import java.util.UUID;
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

  private static void assertWorkspaceAwsFields(
      UFWorkspace expected, UFWorkspace actual, String messageSource) {
    assertEquals(expected.id, actual.id, "workspace id matches that in " + messageSource);
    assertEquals(
        expected.awsMajorVersion,
        actual.awsMajorVersion,
        "workspace aws major version matches that in " + messageSource);
    assertEquals(
        expected.awsOrganizationId,
        actual.awsOrganizationId,
        "workspace aws organization id matches that in " + messageSource);
    assertEquals(
        expected.awsAccountId,
        actual.awsAccountId,
        "workspace aws account id matches that in " + messageSource);
    assertEquals(
        expected.awsTenantAlias,
        actual.awsTenantAlias,
        "workspace aws tenant alias matches that in " + messageSource);
    assertEquals(
        expected.awsEnvironmentAlias,
        actual.awsEnvironmentAlias,
        "workspace aws environment alias matches that in " + messageSource);
  }

  @Test
  @DisplayName("status and describe list reflect workspace create")
  void statusDescribeListReflectCreate() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // check the created workspace has an id and aws details
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(
        createdWorkspace.awsMajorVersion, "create workspace returned a aws major version");
    assertNotNull(
        createdWorkspace.awsOrganizationId, "create workspace returned a aws organization id");
    assertNotNull(createdWorkspace.awsAccountId, "create workspace returned a aws account id");
    assertNotNull(createdWorkspace.awsTenantAlias, "create workspace returned a aws tenant alias");
    assertNotNull(
        createdWorkspace.awsEnvironmentAlias, "create workspace returned a aws environment alias");
    assertThat(
        "workspace email matches test user",
        createdWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));
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
    assertWorkspaceAwsFields(createdWorkspace, status.workspace, "current status");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertWorkspaceAwsFields(createdWorkspace, describedWorkspace, "describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertWorkspaceAwsFields(createdWorkspace, (UFWorkspace) matchingWorkspaces.get(0), "list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("AWS workspace describe reflects the number of resources")
  void describeReflectsNumResources() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));
    assertEquals(0, createdWorkspace.numResources, "new workspace has 0 resources");

    // TODO-Dex --bucket-name ?
    // `terra resource create aws-storage-folder --name=$name --bucket-name=$bucketName`
    String resourceName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "aws-storage-folder", "--name=" + resourceName);

    // `terra workspace describe`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        1,
        describedWorkspace.numResources,
        "workspace has 1 resource after creating storage folder");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
