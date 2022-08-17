package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFLoggingConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get logging" command. */
@Command(name = "logging", description = "Get the logging level.")
public class Logging extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  public static void printText(UFLoggingConfig returnValue) {
    OUT.println(
        "[logging, console] logging level for printing directly to the terminal = "
            + returnValue.consoleLoggingLevel);
    OUT.println(
        "[logging, file] logging level for writing to files in "
            + Context.getLogFile().getParent()
            + " = "
            + returnValue.fileLoggingLevel);
  }

  /** Return the logging level properties of the global context. */
  @Override
  protected void execute() {
    UFLoggingConfig loggingLevels =
        new UFLoggingConfig.Builder()
            .consoleLoggingLevel(Context.getConfig().getConsoleLoggingLevel())
            .fileLoggingLevel(Context.getConfig().getFileLoggingLevel())
            .build();
    formatOption.printReturnValue(loggingLevels, Logging::printText);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
