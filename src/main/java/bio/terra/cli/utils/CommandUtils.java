package bio.terra.cli.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.FeatureService;
import bio.terra.workspace.model.CloudPlatform;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandUtils {
  public static boolean isPlatformEnabled(CloudPlatform cloudPlatform) {
    boolean isEnabled =
        switch (cloudPlatform) {
          case AWS -> FeatureService.fromContext()
              .isFeatureEnabled(FeatureService.AWS_ENABLED, Context.requireUser().getEmail());
          case GCP -> true;
          default -> false;
        };

    return isEnabled
        ||
        // fallback: if feature service is not enabled check server config
        Context.getServer().getSupportedCloudPlatforms().contains(cloudPlatform);
  }

  public static void checkPlatformEnabled(CloudPlatform cloudPlatform)
      throws UserActionableException {
    if (!isPlatformEnabled(cloudPlatform)) {
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
          "Workspace does not support operations on cloud platforms: "
              + Arrays.stream(cloudPlatforms)
                  .map(CloudPlatform::getValue)
                  .collect(Collectors.joining(", ")));
    }
  }

  // Checks if the dataproc is supported
  public static void checkDataprocSupport() throws UserActionableException {
    if (!FeatureService.fromContext()
        .isFeatureEnabled(FeatureService.CLI_DATAPROC_ENABLED, Context.requireUser().getEmail())) {
      throw new UserActionableException("Dataproc is not enabled for the current server or user.");
    }
  }
}
