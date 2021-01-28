package bio.terra.cli.app.supported;

import bio.terra.cli.app.AppsManager;
import bio.terra.cli.model.GlobalContext;

/**
 * This class contains logic related to running the supported tool gcloud. There's no need for any
 * special setup or teardown logic since gcloud is already initialized when the container starts.
 */
public class GcloudHelper implements SupportedToolHelper {

  /** Run the tool inside the Docker container for external applications/tools. */
  public String run(GlobalContext globalContext, String[] cmdArgs) {
    String fullCommand = buildFullCommand("gcloud", cmdArgs);
    return new AppsManager(globalContext).runAppCommand(fullCommand);
  }
}
