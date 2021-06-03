package bio.terra.cli.serialization.command;

import bio.terra.cli.Config;
import bio.terra.cli.Context;
import bio.terra.cli.Server;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a configuration for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Config} class for a configuration's internal representation.
 */
@JsonDeserialize(builder = CommandConfig.Builder.class)
public class CommandConfig {
  public final Config.BrowserLaunchOption browserLaunchOption;
  public final Config.CommandRunnerOption commandRunnerOption;
  public final String dockerImageId;
  public final int resourcesCacheSize;
  public final Logger.LogLevel fileLoggingLevel;
  public final Logger.LogLevel consoleLoggingLevel;
  public final String serverName;

  /** Serialize an instance of the internal class to the command format. */
  public CommandConfig(Config internalConfig, Server internalServer) {
    this.browserLaunchOption = internalConfig.getBrowserLaunchOption();
    this.commandRunnerOption = internalConfig.getCommandRunnerOption();
    this.dockerImageId = internalConfig.getDockerImageId();
    this.resourcesCacheSize = internalConfig.getResourcesCacheSize();
    this.fileLoggingLevel = internalConfig.getFileLoggingLevel();
    this.consoleLoggingLevel = internalConfig.getConsoleLoggingLevel();
    this.serverName = internalServer.getName();
  }

  /** Constructor for Jackson deserialization during testing. */
  private CommandConfig(Builder builder) {
    this.browserLaunchOption = builder.browserLaunchOption;
    this.commandRunnerOption = builder.commandRunnerOption;
    this.dockerImageId = builder.dockerImageId;
    this.resourcesCacheSize = builder.resourcesCacheSize;
    this.fileLoggingLevel = builder.fileLoggingLevel;
    this.consoleLoggingLevel = builder.consoleLoggingLevel;
    this.serverName = builder.serverName;
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

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private Config.BrowserLaunchOption browserLaunchOption;
    private Config.CommandRunnerOption commandRunnerOption;
    private String dockerImageId;
    private int resourcesCacheSize;
    private Logger.LogLevel fileLoggingLevel;
    private Logger.LogLevel consoleLoggingLevel;
    private String serverName;

    public Builder browserLaunchOption(Config.BrowserLaunchOption browserLaunchOption) {
      this.browserLaunchOption = browserLaunchOption;
      return this;
    }

    public Builder commandRunnerOption(Config.CommandRunnerOption commandRunnerOption) {
      this.commandRunnerOption = commandRunnerOption;
      return this;
    }

    public Builder dockerImageId(String dockerImageId) {
      this.dockerImageId = dockerImageId;
      return this;
    }

    public Builder resourcesCacheSize(int resourcesCacheSize) {
      this.resourcesCacheSize = resourcesCacheSize;
      return this;
    }

    public Builder fileLoggingLevel(Logger.LogLevel fileLoggingLevel) {
      this.fileLoggingLevel = fileLoggingLevel;
      return this;
    }

    public Builder consoleLoggingLevel(Logger.LogLevel consoleLoggingLevel) {
      this.consoleLoggingLevel = consoleLoggingLevel;
      return this;
    }

    public Builder serverName(String serverName) {
      this.serverName = serverName;
      return this;
    }

    /** Call the private constructor. */
    public CommandConfig build() {
      return new CommandConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
