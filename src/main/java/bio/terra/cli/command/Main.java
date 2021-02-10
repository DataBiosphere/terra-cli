package bio.terra.cli.command;

import bio.terra.cli.command.app.passthrough.Bq;
import bio.terra.cli.command.app.passthrough.Gcloud;
import bio.terra.cli.command.app.passthrough.Gsutil;
import bio.terra.cli.command.app.passthrough.Nextflow;
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
      App.class,
      Gcloud.class,
      Gsutil.class,
      Bq.class,
      Nextflow.class,
      Notebooks.class
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
    CommandLine cmd = new CommandLine(new Main());
    cmd.setExecutionStrategy(new CommandLine.RunLast());
    // cmd.setExecutionExceptionHandler(new PrintExceptionMessageHandler());

    // TODO: Can we only set this for the app commands, where a random command string follows?
    // It would be good to allow mixing options and parameters for other commands.
    cmd.setStopAtPositional(true);

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
  private static class PrintExceptionMessageHandler
      implements CommandLine.IExecutionExceptionHandler {
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {

      cmd.getErr().println(ex.getMessage());

      return cmd.getExitCodeExceptionMapper() != null
          ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
          : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
  }
}
