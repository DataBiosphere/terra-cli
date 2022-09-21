package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.utils.Logger;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
  // "id" instead of "userFacingId" because user sees this with "terra config list"
  public final String workspaceId;
  public final Format.FormatOptions format;

  public List<UFConfigItem> items = new ArrayList<>();

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
    this.workspaceId = internalWorkspace.map(Workspace::getUserFacingId).orElse(null);
    this.format = internalConfig.getFormat();

    buildItems();
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
    this.format = builder.format;

    buildItems();
  }

  public void buildItems() {
    this.items =
        new ArrayList<>(
            List.of(
                new UFConfigItem(
                    "app-launch", this.commandRunnerOption.toString(), "app launch mode"),
                new UFConfigItem(
                    "browser", this.browserLaunchOption.toString(), "browser launch for login"),
                new UFConfigItem("image", this.dockerImageId, "docker image id"),
                new UFConfigItem(
                    "resource-limit",
                    String.valueOf(this.resourcesCacheSize),
                    "max number of resources to allow per workspace"),
                new UFConfigItem(
                    "console-logging",
                    this.consoleLoggingLevel.toString(),
                    "logging level for printing directly to the terminal"),
                new UFConfigItem(
                    "file-logging",
                    this.fileLoggingLevel.toString(),
                    "logging level for writing to files" + Context.getLogFile().getParent()),
                new UFConfigItem("server", this.serverName, ""),
                new UFConfigItem("workspace", (this.workspaceId), ""),
                new UFConfigItem("format", this.format.toString(), "output format")));
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
    // "id" instead of "userFacingId" because user sees this with "terra config list"
    private String workspaceId;
    private Format.FormatOptions format;

    /** Default constructor for Jackson. */
    public Builder() {}

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

    public Builder workspaceId(String workspaceId) {
      this.workspaceId = workspaceId;
      return this;
    }

    public Builder format(Format.FormatOptions format) {
      this.format = format;
      return this;
    }

    /** Call the private constructor. */
    public UFConfig build() {
      return new UFConfig(this);
    }
  }
}
