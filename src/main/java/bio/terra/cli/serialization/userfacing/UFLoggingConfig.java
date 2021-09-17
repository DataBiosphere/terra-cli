package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.Logger;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the logging config for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFLoggingConfig.Builder.class)
public class UFLoggingConfig implements UserFacing {
  // global logging context = log levels for file and stdout
  public final Logger.LogLevel consoleLoggingLevel;
  public final Logger.LogLevel fileLoggingLevel;

  private UFLoggingConfig(Builder builder) {
    this.consoleLoggingLevel = builder.consoleLoggingLevel;
    this.fileLoggingLevel = builder.fileLoggingLevel;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private Logger.LogLevel consoleLoggingLevel;
    private Logger.LogLevel fileLoggingLevel;

    public Builder consoleLoggingLevel(Logger.LogLevel consoleLoggingLevel) {
      this.consoleLoggingLevel = consoleLoggingLevel;
      return this;
    }

    public Builder fileLoggingLevel(Logger.LogLevel fileLoggingLevel) {
      this.fileLoggingLevel = fileLoggingLevel;
      return this;
    }

    /** Call the private constructor. */
    public UFLoggingConfig build() {
      return new UFLoggingConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
