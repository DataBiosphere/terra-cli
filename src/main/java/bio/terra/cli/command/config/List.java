package bio.terra.cli.command.config;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(List.class);

  @CommandLine.Mixin Format formatOption;

  /** Print out a list of all the config properties. */
  @Override
  protected void execute() {
    logger.debug("terra config list");
    formatOption.printReturnValue(
        new UFConfig(Context.getConfig(), Context.getServer(), Context.getWorkspace()),
        List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFConfig returnValue) {
    returnValue.print();
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
