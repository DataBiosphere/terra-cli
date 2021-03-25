package bio.terra.cli.command.helperclasses;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.context.utils.Logger;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * Base class for all commands. This class handles:
 *
 * <p>- reading in the current global and workspace context
 *
 * <p>- setting up logging
 *
 * <p>- executing the command
 *
 * <p>Sub-classes define how to execute the command (i.e. the implementation of {@link #execute}).
 */
public abstract class BaseCommand implements Callable<Integer> {
  protected GlobalContext globalContext;
  protected WorkspaceContext workspaceContext;

  // output stream to use for writing command return value
  protected static final PrintStream OUT = System.out;

  @Override
  public Integer call() {
    // read in the global context and setup logging
    globalContext = GlobalContext.readFromFile();
    new Logger(globalContext).setupLogging();

    // read in the workspace context
    workspaceContext = WorkspaceContext.readFromFile();

    // do the login flow if required
    if (requiresLogin()) {
      new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    }

    // execute the command
    execute();

    // set the command exit code
    return 0;
  }

  /**
   * Required override for executing this command and printing any output.
   *
   * <p>Sub-classes should throw exceptions for errors. The Main class handles turning these
   * exceptions into the appropriate exit code.
   */
  protected abstract void execute();

  /**
   * This method returns true if login is required for the command. Default implementation is to
   * always require login.
   *
   * <p>Sub-classes can use the current global and workspace context to decide whether to require
   * login.
   *
   * @return true if login is required for this command
   */
  protected boolean requiresLogin() {
    return true;
  }
}
