package harness.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import harness.CRLJanitor;
import harness.TestCommand;
import harness.TestUser;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utilities for working with workspaces in CLI tests. */
public class WorkspaceUtils {
  public static String createUserFacingId() {
    return "a-" + UUID.randomUUID();
  }

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled. Tests must use this method in order to register workspaces with
   * Janitor, direct calls to `terra workspace create` will potentially leak workspaces.
   *
   * @param workspaceCreator The user who owns the workspace. This user will be impersonated to in
   *     the WSM workspaceDelete request.
   */
  public static UFWorkspace createWorkspace(
      TestUser workspaceCreator, Optional<CloudPlatform> platform)
      throws IOException, InterruptedException {
    // `terra workspace create --format=json`
    List<String> argsList =
        Stream.of("workspace", "create", "--id=" + createUserFacingId())
            .collect(Collectors.toList());

    // defaults to GCP otherwise
    platform.ifPresent(cloudPlatform -> argsList.add("--platform=" + cloudPlatform));

    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, argsList.toArray(new String[0]));
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
      throws IOException, InterruptedException {
    return createWorkspace(workspaceCreator, name, description, properties, Optional.empty());
  }

  /**
   * Create a new workspace and register it with Janitor if this test is running in an environment
   * where Janitor is enabled.
   *
   * @param workspaceCreator The user who owns the workspace. This user will be impersonated to in
   *     the WSM workspaceDelete request.
   */
  public static UFWorkspace createWorkspace(
      TestUser workspaceCreator,
      String name,
      String description,
      String properties,
      Optional<CloudPlatform> platform)
      throws IOException, InterruptedException {
    // `terra workspace create --format=json --name=$name --description=$description`
    List<String> argsList =
        Stream.of(
                "workspace",
                "create",
                "--id=" + createUserFacingId(),
                "--name=" + name,
                "--description=" + description,
                "--properties=" + properties)
            .collect(Collectors.toList());

    // defaults to GCP otherwise
    platform.ifPresent(cloudPlatform -> argsList.add("--platform=" + cloudPlatform));

    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, argsList.toArray(new String[0]));
    CRLJanitor.registerWorkspaceForCleanup(getUuidFromCurrentWorkspace(), workspaceCreator);
    waitForCloudSync(workspaceCreator, workspace);
    return workspace;
  }

  /**
   * UFWorkspace doesn't have UUID because user only sees userFacingId. Get UUID from context.json
   * instead. This assumes there is a current workspace.
   */
  private static UUID getUuidFromCurrentWorkspace() {
    return Context.requireWorkspace().getUuid();
  }

  /**
   * Poll the underlying workspace GCP project until the test user has a token permission (list GCS
   * buckets in this case). This helps hide delay in syncing cloud IAM bindings.
   */
  private static void waitForCloudSync(TestUser workspaceCreator, UFWorkspace workspace)
          throws IOException, InterruptedException {
    var creatorCredentials = workspaceCreator.getCredentialsWithCloudPlatformScope();
    Storage storageClient =
            StorageOptions.newBuilder()
                    .setProjectId(workspace.googleProjectId)
                    .setCredentials(creatorCredentials)
                    .build()
                    .getService();
    CrlUtils.callGcpWithPermissionExceptionRetries(storageClient::list);
  }

  /**
   * Helper method to call `terra workspace list` and filter the results on the specified workspace
   * id. Use a high limit to ensure that leaked workspaces in the list don't cause the one we care
   * about to page out.
   */
  public static List<UFWorkspaceLight> listWorkspacesWithId(String userFacingId)
      throws JsonProcessingException {
    // `terra workspace list --format=json --limit=500`
    List<UFWorkspaceLight> listWorkspaces =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list", "--limit=500");

    return listWorkspaces.stream()
        .filter(workspace -> workspace.id.equals(userFacingId))
        .collect(Collectors.toList());
  }
}
