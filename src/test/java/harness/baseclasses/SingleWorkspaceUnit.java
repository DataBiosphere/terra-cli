package harness.baseclasses;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUser;
import harness.utils.WorkspaceUtils;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for unit tests that only need a single workspace for all test methods. This makes the
 * tests run faster because we don't have to create a new workspace for each method. It does mean
 * we're not starting with a completely clean state each time, but that's easy to do just for
 * debugging a particular failure.
 */
public class SingleWorkspaceUnit extends ClearContextUnit {
  protected static final TestUser workspaceCreator = TestUser.chooseTestUserWithSpendAccess();
  private static UUID workspaceId;

  protected static UUID getWorkspaceId() {
    return workspaceId;
  }

  @BeforeAll
  protected void setupOnce() throws Exception {
    TestContext.clearGlobalContextDir();
    resetContext();

    workspaceCreator.login();

    UFWorkspace createdWorkspace = WorkspaceUtils.createWorkspace(workspaceCreator);
    workspaceId = createdWorkspace.id;
  }

  @AfterAll
  protected void cleanupOnce() throws Exception {
    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspaceId);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
