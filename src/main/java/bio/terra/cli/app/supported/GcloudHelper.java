package bio.terra.cli.app.supported;

import bio.terra.cli.app.ToolsManager;

/**
 * This class contains logic related to running the supported tool gcloud. There's no need for any
 * special setup or teardown logic since gcloud is already initialized when the container starts.
 */
public class GcloudHelper extends SupportedToolHelper {

  /** Run gcloud in the Docker container. */
  public String run(String[] cmdArgs) {
    String fullCommand = buildFullCommand("gcloud", cmdArgs);
    return new ToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
