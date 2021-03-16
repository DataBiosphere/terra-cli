package bio.terra.cli.command;

import bio.terra.cli.command.app.passthrough.Bq;
import bio.terra.cli.command.app.passthrough.Gcloud;
import bio.terra.cli.command.app.passthrough.Gsutil;
import bio.terra.cli.command.app.passthrough.Nextflow;
import bio.terra.cli.command.exception.InternalErrorException;
import bio.terra.cli.command.exception.UserFacingException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.utils.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;

/**
 * This class corresponds to the top-level "terra" command. It is also the entry-point for the
 * picocli library.
 */
@Command(
    name = "terra",
    subcommands = {
      Status.class,
      Auth.class,
      Server.class,
      Workspace.class,
      Resources.class,
      DataRefs.class,
      App.class,
      Gcloud.class,
      Gsutil.class,
      Bq.class,
      Nextflow.class,
      Notebooks.class,
      Groups.class,
      Spend.class
    },
    description = "Terra CLI")
class Main implements Runnable {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

  /**
   * Main entry point into the CLI application. For picocli, this creates and executes the top-level
   * command Main.
   *
   * @param args from stdin
   */
  public static void main(String... args) {
    // TODO (PF-446): Can we move this to a common base class so we only read the global context
    // file once?
    GlobalContext globalContext = GlobalContext.readFromFile();
    new Logger(globalContext).setupLogging();

    CommandLine cmd = new CommandLine(new Main());
    cmd.setExecutionStrategy(new CommandLine.RunLast());
    cmd.setExecutionExceptionHandler(new UserFacingExceptionHandler());

    // TODO: Can we only set this for the app commands, where a random command string follows?
    // It would be good to allow mixing options and parameters for other commands.
    cmd.setStopAtPositional(true);

    // delegate to the appropriate command class, or print the usage if no command was specified
    cmd.execute(args);
    if (args.length == 0) {
      cmd.usage(System.out);
    }
  }

  /** Required method to implement Runnable, but not actually called by picocli. */
  @Override
  public void run() {}

  /**
   * Custom handler class that intercepts all exceptions.
   *
   * <p>There are three categories of exceptions. All print a message to stderr and log the
   * exception.
   *
   * <p>- User-facing = user can fix
   *
   * <p>- Internal = user cannot fix, exception specifically thrown by CLI code
   *
   * <p>- Unexpected = user cannot fix, exception not thrown by CLI code
   *
   * <p>The internal and unexpected cases are very similar, except that the message on the internal
   * exception might be more readable/relevant.
   */
  private static class UserFacingExceptionHandler
      implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
      String errorMessage;
      boolean printPointerToLogFile;
      if (ex instanceof UserFacingException) {
        errorMessage = ex.getMessage();
        printPointerToLogFile = false;
      } else if (ex instanceof InternalErrorException) {
        errorMessage =
            ex.getMessage() + (ex.getCause() != null ? ": " + ex.getCause().getMessage() : "");
        printPointerToLogFile = true;
      } else {
        errorMessage = ex.getClass().getCanonicalName() + ": " + ex.getMessage();
        printPointerToLogFile = true;
      }

      // print the error for the user
      printErrorText(cmd, "[ERROR] " + errorMessage);
      if (printPointerToLogFile) {
        printErrorText(cmd, "See $HOME/.terra/terra.log for more information");
      }

      // log the exact message that was printed to the console, for easier debugging
      logger.error(errorMessage, ex);

      // set the process return code
      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    }

    /**
     * Helper method to print a message to stdout or stderr, in red text.
     *
     * @param message string to print
     */
    private void printErrorText(CommandLine commandLine, String message) {
      commandLine.getErr().println(commandLine.getColorScheme().errorText(message));
    }
  }
}
