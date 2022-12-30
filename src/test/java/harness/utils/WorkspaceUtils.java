package harness.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.CRLJanitor;
import harness.TestCommand;
import harness.TestUser;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Utilities for working with workspaces in CLI tests. */
public class WorkspaceUtils {

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
  public static UFWorkspace createWorkspace(TestUser workspaceCreator)
      throws JsonProcessingException {
    return createWorkspace(workspaceCreator, Optional.empty());
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
      TestUser workspaceCreator, Optional<CloudPlatform> platform) throws JsonProcessingException {
    // `terra workspace create --format=json`
    List<String> argsList = Arrays.asList("workspace", "create", "--id=" + createUserFacingId());
    if (platform.isPresent()) { // defaults to GCP otherwise
      argsList.add("--platform=" + platform.get());
    }

    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, argsList.toArray(new String[0]));
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
      TestUser workspaceCreator, String name, String description, String properties)
      throws JsonProcessingException {
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
      throws JsonProcessingException {
    // `terra workspace create --format=json --name=$name --description=$description`
    List<String> argsList =
        Arrays.asList(
            "workspace",
            "create",
            "--id=" + createUserFacingId(),
            "--name=" + name,
            "--description=" + description,
            "--properties=" + properties);
    if (platform.isPresent()) { // defaults to GCP otherwise
      argsList.add("--platform=" + platform.get());
    }

    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, argsList.toArray(new String[0]));
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
