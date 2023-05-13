package harness.baseclasses;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestContext;
import harness.utils.WorkspaceUtils;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for unit tests that only need a single workspace for all test methods. This makes the
 * tests run faster because we don't have to create a new workspace for each method. It does mean
 * we're not starting with a completely clean state each time, but that's easy to do just for
 * debugging a particular failure.
 */
public class SingleWorkspaceUnit extends ClearContextUnit {
  private static String userFacingId;

  protected static String getUserFacingId() {
    return userFacingId;
  }

  @BeforeAll
  protected void setupOnce() throws Exception {
    super.setupOnce();
    workspaceCreator.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));
    userFacingId = createdWorkspace.id;
  }

  @AfterAll
  protected void cleanupOnce() throws Exception {
    if (userFacingId == null) { // nothing to clean up
      return;
    }

    TestContext.clearGlobalContextDir();
    resetContext();

    // login as the same user that created the workspace
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + userFacingId);

    // `terra workspace delete`
    // Use retries since delete resources within workspace may take few minutes
    TestCommand.runCommandExpectSuccessWithRetries("workspace", "delete", "--quiet");
  }

  protected void createBucket(String resourceName, String bucketName) {
    // `terra resource create [bucket-type] --name=$name --bucket-name=$bucketName --format=json`
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        getPlatformStorageName(),
        "--name=" + resourceName,
        "--bucket-name=" + bucketName);
  }
}
