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

  @Test
  @DisplayName("status, describe, AWS workspace list reflect workspace create")
  void statusDescribeListReflectCreateAws() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // check the created workspace has an id and a google project
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(
        createdWorkspace.awsAccountNumber, "create workspace returned a aws account number");
    assertNotNull(
        createdWorkspace.landingZoneId, "create workspace returned a aws landing zone id");
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
        createdWorkspace.awsAccountNumber,
        status.workspace.awsAccountNumber,
        "workspace aws account number matches current status");
    assertEquals(
        createdWorkspace.landingZoneId,
        status.workspace.landingZoneId,
        "workspace aws landing zone id matches current status");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertEquals(
        createdWorkspace.id, describedWorkspace.id, "workspace id matches that in describe");
    assertEquals(
        createdWorkspace.awsAccountNumber,
        describedWorkspace.awsAccountNumber,
        "workspace aws account number matches that in describe");
    assertEquals(
        createdWorkspace.landingZoneId,
        describedWorkspace.landingZoneId,
        "workspace aws landing zone id matches that in describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertEquals(
        createdWorkspace.id, matchingWorkspaces.get(0).id, "workspace id matches that in list");
    assertEquals(
        createdWorkspace.awsAccountNumber,
        matchingWorkspaces.get(0).awsAccountNumber,
        "workspace aws account number matches that in list");
    assertEquals(
        createdWorkspace.landingZoneId,
        matchingWorkspaces.get(0).landingZoneId,
        "workspace aws landing zone id matches that in list");

    // `terra workspace delete`
    // TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("AWS workspace describe reflects the number of resources")
  void describeReflectsNumResourcesAws() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));
    assertEquals(0, createdWorkspace.numResources, "new workspace has 0 resources");

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName`
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess("resource", "create", "aws-bucket", "--name=" + bucketName);

    // `terra workspace describe`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        1, describedWorkspace.numResources, "workspace has 1 resource after creating bucket");

    // `terra workspace delete`
    // TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
