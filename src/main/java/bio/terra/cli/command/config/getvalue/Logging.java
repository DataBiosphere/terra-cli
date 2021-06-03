package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandLoggingConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value logging" command. */
@Command(name = "logging", description = "Get the logging level.")
public class Logging extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Return the logging level properties of the global context. */
  @Override
  protected void execute() {
    CommandLoggingConfig loggingLevels =
        new CommandLoggingConfig.Builder()
            .consoleLoggingLevel(Context.getConfig().getConsoleLoggingLevel())
            .fileLoggingLevel(Context.getConfig().getFileLoggingLevel())
            .build();
    formatOption.printReturnValue(loggingLevels, Logging::printText);
  }

  /** Print this command's output in text format. */
  public static void printText(CommandLoggingConfig returnValue) {
    OUT.println(
        "[logging, console] logging level for printing directly to the terminal = "
            + returnValue.consoleLoggingLevel);
    OUT.println(
        "[logging, file] logging level for writing to files in "
            + Context.getLogFile().getParent()
            + " = "
            + returnValue.fileLoggingLevel);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
