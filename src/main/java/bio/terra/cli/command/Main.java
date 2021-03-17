package bio.terra.cli.command;

import bio.terra.cli.command.app.passthrough.Bq;
import bio.terra.cli.command.app.passthrough.Gcloud;
import bio.terra.cli.command.app.passthrough.Gsutil;
import bio.terra.cli.command.app.passthrough.Nextflow;
import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.command.exception.UserActionableException;
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
      Version.class,
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

  // color scheme used by all commands
  private static final CommandLine.Help.ColorScheme colorScheme =
      new CommandLine.Help.ColorScheme.Builder()
          .commands(CommandLine.Help.Ansi.Style.bold)
          .options(CommandLine.Help.Ansi.Style.fg_yellow)
          .parameters(CommandLine.Help.Ansi.Style.fg_yellow)
          .optionParams(CommandLine.Help.Ansi.Style.italic)
          .errors(CommandLine.Help.Ansi.Style.fg_blue)
          .stackTraces(CommandLine.Help.Ansi.Style.italic)
          .build();

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
    cmd.setExecutionExceptionHandler(new UserActionableAndSystemExceptionHandler());
    cmd.setColorScheme(colorScheme);

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
   * <p>- UserActionable = user can fix
   *
   * <p>- System = user cannot fix, exception specifically thrown by CLI code
   *
   * <p>- Unexpected = user cannot fix, exception not thrown by CLI code
   *
   * <p>The System and Unexpected cases are very similar, except that the message on the system
   * exception might be more readable/relevant.
   */
  private static class UserActionableAndSystemExceptionHandler
      implements CommandLine.IExecutionExceptionHandler {

    // color scheme used for printing out system and unexpected errors
    // (there is only a single error style that you can define for all commands, and we are already
    // using that for user-actionable errors)
    private static final CommandLine.Help.ColorScheme systemAndUnexpectedErrorStyle =
        new CommandLine.Help.ColorScheme.Builder()
            .errors(CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.bold)
            .build();

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
      String errorMessage;
      CommandLine.Help.Ansi.Text formattedErrorMessage;
      boolean printPointerToLogFile;
      if (ex instanceof UserActionableException) {
        errorMessage = ex.getMessage();
        formattedErrorMessage = cmd.getColorScheme().errorText(errorMessage);
        printPointerToLogFile = false;
      } else if (ex instanceof SystemException) {
        errorMessage =
            ex.getMessage() + (ex.getCause() != null ? ": " + ex.getCause().getMessage() : "");
        formattedErrorMessage =
            systemAndUnexpectedErrorStyle.errorText("[ERROR] ").concat(errorMessage);
        printPointerToLogFile = true;
      } else {
        errorMessage =
            "An unexpected error occurred in "
                + ex.getClass().getCanonicalName()
                + ": "
                + ex.getMessage();
        formattedErrorMessage =
            systemAndUnexpectedErrorStyle.errorText("[ERROR] ").concat(errorMessage);
        printPointerToLogFile = true;
      }

      // print the error for the user
      cmd.getErr().println(formattedErrorMessage);
      if (printPointerToLogFile) {
        cmd.getErr()
            .println(
                cmd.getColorScheme()
                    .stackTraceText("See " + GlobalContext.getLogFile() + " for more information"));
      }

      // log the exact message that was printed to the console, for easier debugging
      logger.error(errorMessage, ex);

      // set the process return code
      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
  }
}
