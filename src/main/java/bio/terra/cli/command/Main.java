package bio.terra.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;

/**
 * This class corresponds to the top-level "terra" command. It is also the entry-point for the
 * picocli library.
 */
@Command(
    name = "terra",
    subcommands = {Auth.class, Server.class, CommandLine.HelpCommand.class},
    description = "MC Terra CLI")
class Main implements Runnable {

  /**
   * Main entry point into the CLI application. For picocli, this creates and executes the top-level
   * command Main.
   *
   * @param args from stdin
   */
  public static void main(String... args) {
    CommandLine cmd = new CommandLine(new Main());
    //            .setExecutionExceptionHandler(new PrintExceptionMessageHandler());
    cmd.setExecutionStrategy(new CommandLine.RunLast());
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
