package bio.terra.cli.command.config;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandConfig;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Print out a list of all the config properties. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        new CommandConfig(Context.getConfig(), Context.getServer()), List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CommandConfig returnValue) {
    OUT.println("[app-launch] app launch mode = " + returnValue.commandRunnerOption);
    OUT.println("[browser] browser launch for login = " + returnValue.browserLaunchOption);
    OUT.println("[image] docker image id = " + returnValue.dockerImageId);
    OUT.println(
        "[resource-limit] max number of resources to allow per workspace = "
            + returnValue.resourcesCacheSize);
    OUT.println();
    OUT.println(
        "[logging, console] logging level for printing directly to the terminal = "
            + returnValue.consoleLoggingLevel);
    OUT.println(
        "[logging, file] logging level for writing to files in "
            + Context.getLogFile().getParent()
            + " = "
            + returnValue.fileLoggingLevel);
    OUT.println();
    OUT.println("[server] server = " + returnValue.serverName);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
