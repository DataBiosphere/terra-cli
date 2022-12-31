package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.workspace.model.CloudPlatform;
import harness.TestCommand;
import harness.TestCommand.Result;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands specific to CloudPlatform.GCP. */
@Tag("unit")
public class GcpWorkspace extends ClearContextUnit {
  private static final Optional<CloudPlatform> platformGcp = Optional.of(CloudPlatform.GCP);

  @Test
  @DisplayName("status, describe, workspace list reflect workspace create")
  void statusDescribeListReflectCreateGcp() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(testUser, platformGcp);

    // check the created workspace has an id and a google project
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(createdWorkspace.googleProjectId, "create workspace created a gcp project");
    assertThat(
        "workspace email matches test user",
        createdWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));

    // check the created workspace has cloud platform set
    assertThat(
        "workspace cloudPlatform matches GCP",
        CloudPlatform.GCP,
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
        createdWorkspace.googleProjectId,
        status.workspace.googleProjectId,
        "workspace gcp project matches current status");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertEquals(
        createdWorkspace.id, describedWorkspace.id, "workspace id matches that in describe");
    assertEquals(
        createdWorkspace.googleProjectId,
        describedWorkspace.googleProjectId,
        "workspace gcp project matches that in describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
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
  @DisplayName("workspace describe reflects the number of resources")
  void describeReflectsNumResourcesGcp() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(testUser, platformGcp);
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
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        1, describedWorkspace.numResources, "worksapce has 1 resource after creating bucket");

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

    UFWorkspace describedWorkspace3 =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        2, describedWorkspace3.numResources, "worksapce has 2 resources after creating dataset");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
