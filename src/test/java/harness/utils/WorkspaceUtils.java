package harness.utils;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.CRLJanitor;
import harness.TestCommand;
import harness.TestUser;

/** Utilities for working with workspaces in CLI tests. */
public class WorkspaceUtils {

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled.
   *
   * @param workspaceCreator The user who owns the workspace and should be used to clean it up.
   */
  public static UFWorkspace createWorkspace(TestUser workspaceCreator)
      throws JsonProcessingException {
    // `terra workspace create --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    CRLJanitor.registerWorkspaceForCleanupIfEnabled(workspace, workspaceCreator);
    return workspace;
  }

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled.
   *
   * @param workspaceCreator The user who owns the workspace and should be used to clean it up.
   * @param name The name of the workspace
   * @param description Description of the workspace
   */
  public static UFWorkspace createWorkspace(
      TestUser workspaceCreator, String name, String description) throws JsonProcessingException {
    // `terra workspace create --format=json --name=$name --description=$description`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class,
            "workspace",
            "create",
            "--name=" + name,
            "--description=" + description);
    CRLJanitor.registerWorkspaceForCleanupIfEnabled(workspace, workspaceCreator);
    return workspace;
  }
}
