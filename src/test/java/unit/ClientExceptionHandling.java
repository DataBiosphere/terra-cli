package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;

import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.SamGroups;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for handling exceptions thrown by Terra services. */
@Tag("unit")
public class ClientExceptionHandling extends SingleWorkspaceUnit {
  final SamGroups trackedGroups = new SamGroups();

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();

    // try to delete each group that was created by a method in this class
    trackedGroups.deleteAllTrackedGroups();
  }

  @Test
  @DisplayName(
      "try to create a group twice, check that the output includes the CLI and SAM error messages")
  void samDuplicateGroup() throws IOException {
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // `terra group create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("group", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, testUser);

    // try to create another group with the same name
    String stdErr = TestCommand.runCommandExpectExitCode(2, "group", "create", "--name=" + name);
    assertThat(
        "stderr includes the CLI error message",
        stdErr,
        CoreMatchers.containsString("Error creating SAM group"));
    assertThat(
        "stderr includes the SAM error message",
        stdErr,
        CoreMatchers.containsString("A resource of this type and name already exists"));
  }

  @Test
  @DisplayName(
      "try to create a BQ controlled resource twice, check that the output includes the CLI and WSM error messages")
  void wsmDuplicateResource() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "listDescribeReflectCreate";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId);

    // try to create another resource with the same name
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            2, "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId);
    assertThat(
        "stderr includes the CLI error message",
        stdErr,
        CoreMatchers.containsString("Error creating controlled BigQuery dataset in the workspace"));
    assertThat(
        "stderr includes the WSM error message",
        stdErr,
        CoreMatchers.containsString("A resource with matching attributes already exists"));
  }
}
