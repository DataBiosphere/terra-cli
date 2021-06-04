package harness.baseclasses;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUsers;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class SingleWorkspaceUnit extends ClearContextUnit {
  protected static final TestUsers workspaceCreator = TestUsers.chooseTestUserWithSpendAccess();;
  private static UUID workspaceId;

  protected static UUID getWorkspaceId() {
    return workspaceId;
  }

  @BeforeAll
  static void setupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    workspaceCreator.login();

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--format=json");
    workspaceId = createWorkspace.id;
  }

  @AfterAll
  static void cleanupOnce() throws IOException {
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
