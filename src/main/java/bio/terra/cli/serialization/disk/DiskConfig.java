package bio.terra.cli.serialization.disk;

import static bio.terra.cli.Config.BrowserLaunchOption;
import static bio.terra.cli.Config.CommandRunnerOption;

import bio.terra.cli.Config;
import bio.terra.cli.utils.Logger;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DiskConfig.Builder.class)
public class DiskConfig {
  public final BrowserLaunchOption browserLaunchOption;
  public final CommandRunnerOption commandRunnerOption;
  public final String dockerImageId;
  public final int resourcesCacheSize;
  public final Logger.LogLevel fileLoggingLevel;
  public final Logger.LogLevel consoleLoggingLevel;

  private DiskConfig(Builder builder) {
    this.browserLaunchOption = builder.browserLaunchOption;
    this.commandRunnerOption = builder.commandRunnerOption;
    this.dockerImageId = builder.dockerImageId;
    this.resourcesCacheSize = builder.resourcesCacheSize;
    this.fileLoggingLevel = builder.fileLoggingLevel;
    this.consoleLoggingLevel = builder.consoleLoggingLevel;
  }

  /** Builder class to construct an immutable Config object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private BrowserLaunchOption browserLaunchOption;
    private CommandRunnerOption commandRunnerOption;
    private String dockerImageId;
    private int resourcesCacheSize;
    private Logger.LogLevel fileLoggingLevel;
    private Logger.LogLevel consoleLoggingLevel;

    public Builder browserLaunchOption(BrowserLaunchOption browserLaunchOption) {
      this.browserLaunchOption = browserLaunchOption;
      return this;
    }

    public Builder commandRunnerOption(CommandRunnerOption commandRunnerOption) {
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

    /** Call the private constructor. */
    public DiskConfig build() {
      return new DiskConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(Config internalObj) {
      this.browserLaunchOption = internalObj.getBrowserLaunchOption();
      this.commandRunnerOption = internalObj.getCommandRunnerOption();
      this.dockerImageId = internalObj.getDockerImageId();
      this.resourcesCacheSize = internalObj.getResourcesCacheSize();
      this.fileLoggingLevel = internalObj.getFileLoggingLevel();
      this.consoleLoggingLevel = internalObj.getConsoleLoggingLevel();
    }
  }
}
