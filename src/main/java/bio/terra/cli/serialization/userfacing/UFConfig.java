package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Optional;
import java.util.UUID;

/**
 * External representation of a configuration for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Config} class for a configuration's internal representation.
 */
@JsonDeserialize(builder = UFConfig.Builder.class)
public class UFConfig {
  public final Config.BrowserLaunchOption browserLaunchOption;
  public final Config.CommandRunnerOption commandRunnerOption;
  public final String dockerImageId;
  public final int resourcesCacheSize;
  public final Logger.LogLevel fileLoggingLevel;
  public final Logger.LogLevel consoleLoggingLevel;
  public final String serverName;
  public final UUID workspaceId;

  /** Serialize an instance of the internal class to the command format. */
  public UFConfig(
      Config internalConfig, Server internalServer, Optional<Workspace> internalWorkspace) {
    this.browserLaunchOption = internalConfig.getBrowserLaunchOption();
    this.commandRunnerOption = internalConfig.getCommandRunnerOption();
    this.dockerImageId = internalConfig.getDockerImageId();
    this.resourcesCacheSize = internalConfig.getResourcesCacheSize();
    this.fileLoggingLevel = internalConfig.getFileLoggingLevel();
    this.consoleLoggingLevel = internalConfig.getConsoleLoggingLevel();
    this.serverName = internalServer.getName();
    this.workspaceId = internalWorkspace.isPresent() ? internalWorkspace.get().getId() : null;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFConfig(Builder builder) {
    this.browserLaunchOption = builder.browserLaunchOption;
    this.commandRunnerOption = builder.commandRunnerOption;
    this.dockerImageId = builder.dockerImageId;
    this.resourcesCacheSize = builder.resourcesCacheSize;
    this.fileLoggingLevel = builder.fileLoggingLevel;
    this.consoleLoggingLevel = builder.consoleLoggingLevel;
    this.serverName = builder.serverName;
    this.workspaceId = builder.workspaceId;
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
    OUT.println("[workspace] workspace = " + (workspaceId == null ? "" : workspaceId));
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
    private UUID workspaceId;

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

    public Builder workspaceId(UUID workspaceId) {
      this.workspaceId = workspaceId;
      return this;
    }

    /** Call the private constructor. */
    public UFConfig build() {
      return new UFConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
