package bio.terra.cli.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.CloudPlatform;

public class CommandUtils {
  public static void checkPlatformSupport(CloudPlatform cloudPlatform)
      throws UserActionableException {
    if (!Context.getServer().getSupportedCloudPlatforms().contains(cloudPlatform)) {
      throw new UserActionableException(
          "CloudPlatform "
              + cloudPlatform
              + " not supported for current user "
              + Context.getUser().get().getEmail()
              + " on the current server "
              + Context.getServer().getName());
    }
  }
}
