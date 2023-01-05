package harness.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.service.utils.CrlUtils;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import harness.CRLJanitor;
import harness.TestCommand;
import harness.TestUser;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for working with workspaces in CLI tests. */
public class WorkspaceUtils {

  private static final Logger logger = LoggerFactory.getLogger(WorkspaceUtils.class);

  public static String createUserFacingId() {
    return "a-" + UUID.randomUUID().toString();
  }

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled. Tests must use this method in order to register workspaces with
   * Janitor, direct calls to `terra workspace create` will potentially leak workspaces.
   *
   * @param workspaceCreator The user who owns the workspace. This user will be impersonated to in
   *     the WSM workspaceDelete request.
   */
  public static UFWorkspace createWorkspace(TestUser workspaceCreator) throws IOException {
    // `terra workspace create --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "create", "--id=" + createUserFacingId());
    CRLJanitor.registerWorkspaceForCleanup(getUuidFromCurrentWorkspace(), workspaceCreator);
    waitForCloudSync(workspaceCreator, workspace);
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
      TestUser workspaceCreator, String name, String description, String properties)
      throws IOException {
    // `terra workspace create --format=json --name=$name --description=$description`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class,
            "workspace",
            "create",
            "--id=" + createUserFacingId(),
            "--name=" + name,
            "--description=" + description,
            "--properties=" + properties);
    CRLJanitor.registerWorkspaceForCleanup(getUuidFromCurrentWorkspace(), workspaceCreator);
    waitForCloudSync(workspaceCreator, workspace);
    return workspace;
  }

  /**
   * Poll the underlying workspace GCP project until the test user has a token permission (list GCS
   * buckets in this case). This helps hide delay in syncing cloud IAM bindings.
   */
  private static void waitForCloudSync(TestUser workspaceCreator, UFWorkspace workspace)
      throws IOException {
    var creatorCredentials = workspaceCreator.getCredentialsWithCloudPlatformScope();
    Storage storageClient =
        StorageOptions.newBuilder()
            .setProjectId(workspace.googleProjectId)
            .setCredentials(creatorCredentials)
            .build()
            .getService();
    try {
      CrlUtils.callGcpWithRetries(storageClient::list);
    } catch (InterruptedException e) {
      logger.info("Interrupted exception!");
    }
  }

  /**
   * UFWorkspace doesn't have UUID because user only sees userFacingId. Get UUID from context.json
   * instead. This assumes there is a current workspace.
   */
  private static UUID getUuidFromCurrentWorkspace() {
    return Context.requireWorkspace().getUuid();
  }
}
