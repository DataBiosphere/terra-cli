package bio.terra.cli.app.supported;

import bio.terra.cli.app.ToolsManager;

/**
 * This class contains logic related to running the supported tool bq. There's no need for any
 * special setup or teardown logic since bq is already initialized when the container starts.
 */
public class BqHelper extends SupportedToolHelper {

  /** Run bq in the Docker container. */
  public String run(String[] cmdArgs) {
    String fullCommand = buildFullCommand("bq", cmdArgs);
    return new ToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
