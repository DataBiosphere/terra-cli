package harness.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.CRLJanitor;
import harness.TestCommand;
import harness.TestUser;
import java.util.UUID;

/** Utilities for working with workspaces in CLI tests. */
public class WorkspaceUtils {

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled. Tests must use this method in order to register workspaces with
   * Janitor, direct calls to `terra workspace create` will potentially leak workspaces.
   *
   * @param workspaceCreator The user who owns the workspace. This user will be impersonated to in
   *     the WSM workspaceDelete request.
   */
  public static UFWorkspace createWorkspace(TestUser workspaceCreator)
      throws JsonProcessingException {
    // `terra workspace create --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    CRLJanitor.registerWorkspaceForCleanup(getUuidFromCurrentWorkspace(), workspaceCreator);
    return workspace;
  }

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled.
   *
   * @param workspaceCreator The user who owns the workspace. This user will be impersonated to in
   *     the WSM workspaceDelete request.
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
    CRLJanitor.registerWorkspaceForCleanup(getUuidFromCurrentWorkspace(), workspaceCreator);
    return workspace;
  }

  /**
   * UFWorkspace doesn't have UUID because user only sees userFacingId. Get UUID from context.json
   * instead. This assumes there is a current workspace.
   */
  private static UUID getUuidFromCurrentWorkspace() {
    return Context.requireWorkspace().getUuid();
  }
}
