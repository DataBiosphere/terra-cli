package bio.terra.cli.command;

import bio.terra.cli.command.app.passthrough.Bq;
import bio.terra.cli.command.app.passthrough.Gcloud;
import bio.terra.cli.command.app.passthrough.Gsutil;
import bio.terra.cli.command.app.passthrough.Nextflow;
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

  /**
   * Main entry point into the CLI application. For picocli, this creates and executes the top-level
   * command Main.
   *
   * @param args from stdin
   */
  public static void main(String... args) {
    // TODO (PF-446): move this to a common base class so we only read the global context file once
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
   * Custom exception handler class that suppresses the stack trace when an exception is thrown.
   * Instead, it just prints the exception message and exits the process.
   */
  private static class UserFacingExceptionHandler
      implements CommandLine.IExecutionExceptionHandler {

    private static final org.slf4j.Logger logger =
        LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
      if (ex instanceof RuntimeException) {
        // print the message, log the stacktrace
        cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
        logger.info("user error " + ex.getClass());
      } else {
        // print generic message, log the stacktrace
        cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
        logger.error("internal error " + ex.getClass());
      }

      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
  }
}
