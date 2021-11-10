package bio.terra.cli.serialization.persisted;

import static bio.terra.cli.businessobject.Config.BrowserLaunchOption;
import static bio.terra.cli.businessobject.Config.CommandRunnerOption;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.utils.Logger;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a configuration for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link Config} class for a configuration's internal representation.
 */
@JsonDeserialize(builder = PDConfig.Builder.class)
public class PDConfig {
  public final BrowserLaunchOption browserLaunchOption;
  public final CommandRunnerOption commandRunnerOption;
  public final String dockerImageId;
  public final int resourcesCacheSize;
  public final Logger.LogLevel fileLoggingLevel;
  public final Logger.LogLevel consoleLoggingLevel;
  public final Format.FormatOptions formatOption;

  /** Serialize an instance of the internal class to the disk format. */
  public PDConfig(Config internalObj) {
    this.browserLaunchOption = internalObj.getBrowserLaunchOption();
    this.commandRunnerOption = internalObj.getCommandRunnerOption();
    this.dockerImageId = internalObj.getDockerImageId();
    this.resourcesCacheSize = internalObj.getResourcesCacheSize();
    this.fileLoggingLevel = internalObj.getFileLoggingLevel();
    this.consoleLoggingLevel = internalObj.getConsoleLoggingLevel();
    this.formatOption = internalObj.getFormatOption();
  }

  private PDConfig(Builder builder) {
    this.browserLaunchOption = builder.browserLaunchOption;
    this.commandRunnerOption = builder.commandRunnerOption;
    this.dockerImageId = builder.dockerImageId;
    this.resourcesCacheSize = builder.resourcesCacheSize;
    this.fileLoggingLevel = builder.fileLoggingLevel;
    this.consoleLoggingLevel = builder.consoleLoggingLevel;
    this.formatOption = builder.formatOption;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private BrowserLaunchOption browserLaunchOption;
    private CommandRunnerOption commandRunnerOption;
    private String dockerImageId;
    private int resourcesCacheSize;
    private Logger.LogLevel fileLoggingLevel;
    private Logger.LogLevel consoleLoggingLevel;
    private Format.FormatOptions formatOption;

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

    public Builder formatOption(Format.FormatOptions formatOption) {
      this.formatOption = formatOption;
      return this;
    }

    /** Call the private constructor. */
    public PDConfig build() {
      return new PDConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
