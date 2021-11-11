package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.Format.FormatOptions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = UFFormatConfig.Builder.class)
public class UFFormatConfig {
  public final Format.FormatOptions format;

  public UFFormatConfig(FormatOptions format) {
    this.format = format;
  }

  private UFFormatConfig(Builder builder) {
    this.format = builder.format;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private Format.FormatOptions format;

    public Builder formatOption(Format.FormatOptions format) {
      this.format = format;
      return this;
    }

    /** Call the private constructor. */
    public UFFormatConfig build() {
      return new UFFormatConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
