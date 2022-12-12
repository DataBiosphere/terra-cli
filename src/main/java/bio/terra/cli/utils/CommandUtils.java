package bio.terra.cli.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.CloudPlatform;

public class CommandUtils {
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
}
