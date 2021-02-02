package bio.terra.cli.app.supported;

import bio.terra.cli.app.ToolsManager;

/**
 * This class contains logic related to running the supported tool gsutil. There's no need for any
 * special setup or teardown logic since gsutil is already initialized when the container starts.
 */
public class GsutilHelper extends SupportedToolHelper {

  /** Run gsutil in the Docker container. */
  public String run(String[] cmdArgs) {
    String fullCommand = buildFullCommand("gsutil", cmdArgs);
    return new ToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
