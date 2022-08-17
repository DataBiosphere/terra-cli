package bio.terra.cli.command.config;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFConfig;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(UFConfig returnValue) {
    returnValue.print();
  }

  /** Print out a list of all the config properties. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        new UFConfig(Context.getConfig(), Context.getServer(), Context.getWorkspace()),
        List::printText);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
