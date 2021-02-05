package bio.terra.cli.app.supported;

import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;

/** This class specifies a multi-command model for using a supported app: enable, run, stop. */
public abstract class SupportedAppHelper {

  GlobalContext globalContext;
  WorkspaceContext workspaceContext;

  /** Set the global and workspace context properties. */
  public final SupportedAppHelper setContext(
      GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
    return this;
  }

  /** Do any command-specific setup. */
  public void enable() {}

  /** Run the tool inside the Docker container for external applications/tools. */
  public abstract String run(String[] cmdArgs);

  /** Do any command-specific teardown. */
  public void stop() {}

  /** Utility method for concatenating a command and its arguments. */
  public static String buildFullCommand(String cmd, String[] cmdArgs) {
    String fullCommand = cmd;
    if (cmdArgs != null && cmdArgs.length > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, cmdArgs);
    }
    return fullCommand;
  }
}
