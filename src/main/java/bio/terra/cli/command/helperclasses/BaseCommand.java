package bio.terra.cli.command.helperclasses;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.context.utils.Logger;
import bio.terra.cli.context.utils.Printer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine;

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
@CommandLine.Command
@SuppressFBWarnings(
    value = {"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"},
    justification =
        "Output streams are static, so the OUT & ERR properties should also be so. Each command "
            + "execution should only instantiate a single BaseCommand sub-class. So in practice, "
            + "the call() instance method will only be called once, and these static pointers to "
            + "the output streams will only be initialized once. And since picocli handles instantiating "
            + "the command classes, we can't set the output streams in the constructor and make them "
            + "static.")
public abstract class BaseCommand implements Callable<Integer> {
  protected GlobalContext globalContext;
  protected WorkspaceContext workspaceContext;

  // output streams for commands to write to
  protected static PrintWriter OUT;
  protected static PrintWriter ERR;

  @Override
  public Integer call() {
    // pull the output streams from the singleton object setup by the top-level Main class
    // in the future, these streams could also be controlled by a global context property
    OUT = Printer.getOut();
    ERR = Printer.getErr();

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
