package bio.terra.cli.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.CloudPlatform;

public class CommandUtils {
  // Checks if current server supports cloud platform
  public static void checkPlatformSupport(CloudPlatform cloudPlatform)
      throws UserActionableException {
    if (!Context.getServer().getSupportedCloudPlatforms().contains(cloudPlatform)) {
      throw new UserActionableException(
          "Cloud platform "
              + cloudPlatform
              + " not supported for server "
              + Context.getServer().getName());
    }
  }

  // Checks if workspace cloud platform is same as required cloud platform
  public static void checkWorkspaceSupport(CloudPlatform requiredCloudPlatform)
      throws UserActionableException {
    if (Context.requireWorkspace().getCloudPlatform() != requiredCloudPlatform) {
      throw new UserActionableException(
          "Workspace does not support operations on cloud platform " + requiredCloudPlatform);
    }
  }
}
