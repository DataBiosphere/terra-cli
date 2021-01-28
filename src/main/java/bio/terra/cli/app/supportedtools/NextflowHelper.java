package bio.terra.cli.app.supportedtools;

import bio.terra.cli.app.AppsManager;
import bio.terra.cli.model.GlobalContext;

/** This class contains logic related to running the supported tool nextflow. */
public class NextflowHelper implements SupportedToolHelper {

  /** Do any command-specific setup. */
  public void enable(GlobalContext globalContext) {}

  /** Run the tool inside the Docker container for external applications/tools. */
  public String run(GlobalContext globalContext, String[] cmdArgs) {
    String fullCommand = buildFullCommand("nextflow", cmdArgs);
    return new AppsManager(globalContext).runAppCommand(fullCommand);
  }

  /** Do any command-specific teardown. */
  public void stop(GlobalContext globalContext) {}
}
