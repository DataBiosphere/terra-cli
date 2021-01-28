package bio.terra.cli.app.supported;

import bio.terra.cli.model.GlobalContext;

public interface SupportedToolHelper {

  /** Do any command-specific setup. */
  default void enable(GlobalContext globalContext) {}

  /** Run the tool inside the Docker container for external applications/tools. */
  String run(GlobalContext globalContext, String[] cmdArgs);

  /** Do any command-specific teardown. */
  default void stop(GlobalContext globalContext) {}

  /** Utility method for concatenating a command and its arguments. */
  default String buildFullCommand(String cmd, String[] cmdArgs) {
    String fullCommand = cmd;
    if (cmdArgs != null && cmdArgs.length > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, cmdArgs);
    }
    return fullCommand;
  }
}
