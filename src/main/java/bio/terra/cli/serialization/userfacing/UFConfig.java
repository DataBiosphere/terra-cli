package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.Printer;
import java.io.PrintStream;

/**
 * External representation of a configuration for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Config} class for a configuration's internal representation.
 */
public class UFConfig {
  public final Config.BrowserLaunchOption browserLaunchOption;
  public final Config.CommandRunnerOption commandRunnerOption;
  public final String dockerImageId;
  public final int resourcesCacheSize;
  public final Logger.LogLevel fileLoggingLevel;
  public final Logger.LogLevel consoleLoggingLevel;
  public final String serverName;

  /** Serialize an instance of the internal class to the command format. */
  public UFConfig(Config internalConfig, Server internalServer) {
    this.browserLaunchOption = internalConfig.getBrowserLaunchOption();
    this.commandRunnerOption = internalConfig.getCommandRunnerOption();
    this.dockerImageId = internalConfig.getDockerImageId();
    this.resourcesCacheSize = internalConfig.getResourcesCacheSize();
    this.fileLoggingLevel = internalConfig.getFileLoggingLevel();
    this.consoleLoggingLevel = internalConfig.getConsoleLoggingLevel();
    this.serverName = internalServer.getName();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println("[app-launch] app launch mode = " + browserLaunchOption);
    OUT.println("[browser] browser launch for login = " + commandRunnerOption);
    OUT.println("[image] docker image id = " + dockerImageId);
    OUT.println(
        "[resource-limit] max number of resources to allow per workspace = " + resourcesCacheSize);
    OUT.println();
    OUT.println(
        "[logging, console] logging level for printing directly to the terminal = "
            + consoleLoggingLevel);
    OUT.println(
        "[logging, file] logging level for writing to files in "
            + Context.getLogFile().getParent()
            + " = "
            + fileLoggingLevel);
    OUT.println();
    OUT.println("[server] server = " + serverName);
  }
}
