package bio.terra.cli.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.CloudPlatform;
import java.util.Arrays;

public class CommandUtils {
  // Checks if current server supports cloud platform
  public static void checkServerSupport(CloudPlatform cloudPlatform)
      throws UserActionableException {
    if (!Context.getServer().getSupportedCloudPlatforms().contains(cloudPlatform)) {
      throw new UserActionableException(
          "Cloud platform "
              + cloudPlatform
              + " not supported for server "
              + Context.getServer().getName());
    }
  }

  // Checks if workspace cloud platform is one of the cloud platforms
  public static void checkWorkspaceSupport(CloudPlatform... cloudPlatforms)
      throws UserActionableException {
    if (!Arrays.asList(cloudPlatforms).contains(Context.requireWorkspace().getCloudPlatform())) {
      throw new UserActionableException(
          "Workspace does not support operations on cloud platforms " + cloudPlatforms);
    }
  }
}
