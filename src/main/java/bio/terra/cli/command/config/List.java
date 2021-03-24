package bio.terra.cli.command.config;

import static bio.terra.cli.command.config.getvalue.Logging.LoggingReturnValue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand {

  @CommandLine.Mixin FormatOption formatOption;

  /** Print out a list of all the config properties. */
  @Override
  protected void execute() {
    // build a map of the config properties
    LoggingReturnValue loggingLevels =
        new LoggingReturnValue(globalContext.consoleLoggingLevel, globalContext.fileLoggingLevel);
    Map<String, Object> configPropertyMap = new HashMap<>();
    configPropertyMap.put("browser", globalContext.browserLaunchOption);
    configPropertyMap.put("image", globalContext.dockerImageId);
    configPropertyMap.put("logging", loggingLevels);

    formatOption.printReturnValue(configPropertyMap, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(Map<String, Object> returnValue) {
    OUT.println("[browser] browser launch for login = " + globalContext.browserLaunchOption);
    OUT.println("[image] docker image id = " + globalContext.dockerImageId);
    OUT.println(
        "[logging] console logging level = "
            + globalContext.consoleLoggingLevel
            + ", file logging level = "
            + globalContext.fileLoggingLevel);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
