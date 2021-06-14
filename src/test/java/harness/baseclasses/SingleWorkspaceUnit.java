package harness.baseclasses;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUsers;
import java.io.IOException;
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
  protected static final TestUsers workspaceCreator = TestUsers.chooseTestUserWithSpendAccess();;
  private static UUID workspaceId;

  protected static UUID getWorkspaceId() {
    return workspaceId;
  }

  @BeforeAll
  protected void setupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    workspaceCreator.login();

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    workspaceId = createWorkspace.id;
  }

  @AfterAll
  protected void cleanupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + workspaceId);

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete");
  }
}
