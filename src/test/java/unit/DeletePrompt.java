package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.SamGroups;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the confirmation prompt for delete commands. */
@Tag("unit")
public class DeletePrompt extends SingleWorkspaceUnit {
  SamGroups trackedGroups = new SamGroups();

  @Override
  @BeforeEach
  protected void setupEachTime() throws IOException {
    super.setupEachTime();
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
  }

  @Override
  @AfterAll
  protected void cleanupOnce() throws Exception {
    // try to delete each group that was created by a method in this class
    trackedGroups.deleteAllTrackedGroups();
    super.cleanupOnce();
  }

  @Test
  @DisplayName("y/yes proceeds")
  void yesResponseProceeds() {
    String resourceName = "yesResponseProceeds";
    createAndDeleteBucketExpectSuccess(resourceName, "y");
    createAndDeleteBucketExpectSuccess(resourceName, "Y");
    createAndDeleteBucketExpectSuccess(resourceName, "yes");
    createAndDeleteBucketExpectSuccess(resourceName, "YES");
    createAndDeleteBucketExpectSuccess(resourceName, "yEs");
  }

  @Test
  @DisplayName("anything other than y/yes aborts")
  void noResponseAborts() {
    String resourceName = "noResponseAborts";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "gcs-bucket",
        "--name=" + resourceName,
        "--bucket-name=" + bucketName);

    deleteResourceExpectAbort(resourceName, "n");
    deleteResourceExpectAbort(resourceName, "N");
    deleteResourceExpectAbort(resourceName, "no");
    deleteResourceExpectAbort(resourceName, "NO");
    deleteResourceExpectAbort(resourceName, "nO");
    deleteResourceExpectAbort(resourceName, "YESWithTrailingCharacters");
    deleteResourceExpectAbort(resourceName, "withLeadingCharactersYES");
    deleteResourceExpectAbort(resourceName, "inMiddleYESCharacters");
    deleteResourceExpectAbort(resourceName, "random characters");
  }

  @Test
  @DisplayName("prompt response is checked for workspace delete")
  void promptResponseCheckedByWorkspace() {
    // `terra workspace delete`
    ByteArrayInputStream stdIn = new ByteArrayInputStream("NO".getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd = TestCommand.runCommand(stdIn, "workspace", "delete");

    // check the abort case for `workspace delete` to confirm the delete prompt option is supported
    // and its helper method is called to abort if the prompt response is negative
    expectAbort(cmd);
  }

  @Test
  @DisplayName("prompt response is checked for groups delete")
  void promptResponseCheckedByGroups() throws JsonProcessingException {
    // `terra group create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("group", "create", "--name=" + name);

    // track the group so we can clean it up after this test method runs
    trackedGroups.trackGroup(name, workspaceCreator);

    // `terra group delete`
    ByteArrayInputStream stdIn = new ByteArrayInputStream("NO".getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd = TestCommand.runCommand(stdIn, "group", "delete", "--name=" + name);

    // check the abort case for `groups delete` to confirm the delete prompt option is supported and
    // its helper method is called to abort if the prompt response is negative
    expectAbort(cmd);
  }

  /**
   * Create a controlled GCS bucket resource and then delete it with the given prompt response.
   * Expects the delete to succeed.
   */
  private void createAndDeleteBucketExpectSuccess(String resourceName, String promptResponse) {
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "gcs-bucket",
        "--name=" + resourceName,
        "--bucket-name=" + bucketName);

    ByteArrayInputStream stdIn =
        new ByteArrayInputStream(promptResponse.getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd =
        TestCommand.runCommand(stdIn, "resource", "delete", "--name=" + resourceName);
    assertEquals(0, cmd.exitCode, "delete command returned successfully");
  }

  /**
   * Try deleting a resource with the given prompt response. Expects the command to throw a user
   * actionable exception because the prompt response was negative.
   */
  private void deleteResourceExpectAbort(String resourceName, String promptResponse) {
    ByteArrayInputStream stdIn =
        new ByteArrayInputStream(promptResponse.getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd =
        TestCommand.runCommand(stdIn, "resource", "delete", "--name=" + resourceName);
    expectAbort(cmd);
  }

  /** Expect a user actionable exception because the prompt response was negative. */
  private void expectAbort(TestCommand.Result cmd) {
    assertEquals(1, cmd.exitCode, "delete command threw a user actionable exception");
    assertThat(
        "output message says the delete was aborted",
        cmd.stdErr,
        CoreMatchers.containsString("Delete aborted"));
  }
}
