package bio.terra.cli.command.config;

import static bio.terra.cli.command.config.getvalue.Logging.LoggingReturnValue;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.config.getvalue.Logging;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
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
    LoggingReturnValue loggingLevels =
        new LoggingReturnValue(globalContext.consoleLoggingLevel, globalContext.fileLoggingLevel);
    ConfigListReturnValue configList =
        new ConfigListReturnValue(
            globalContext.browserLaunchOption, globalContext.dockerImageId, loggingLevels);

    formatOption.printReturnValue(configList, List::printText);
  }

  /** POJO class for printing out this command's output. */
  private static class ConfigListReturnValue {
    public AuthenticationManager.BrowserLaunchOption browser;
    public String image;
    public LoggingReturnValue logging;

    public ConfigListReturnValue(
        AuthenticationManager.BrowserLaunchOption browser,
        String image,
        LoggingReturnValue logging) {
      this.browser = browser;
      this.image = image;
      this.logging = logging;
    }
  }

  /** Print this command's output in text format. */
  private static void printText(ConfigListReturnValue returnValue) {
    OUT.println("[browser] browser launch for login = " + returnValue.browser);
    OUT.println("[image] docker image id = " + returnValue.image);
    OUT.println();
    Logging.printText(returnValue.logging);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
