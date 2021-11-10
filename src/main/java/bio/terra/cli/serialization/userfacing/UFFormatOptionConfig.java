package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.Format.FormatOptions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = UFFormatOptionConfig.Builder.class)
public class UFFormatOptionConfig {
  public final Format.FormatOptions formatOption;

  public UFFormatOptionConfig(FormatOptions formatOption) {
    this.formatOption = formatOption;
  }

  private UFFormatOptionConfig(Builder builder) {
    this.formatOption = builder.formatOption;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private Format.FormatOptions formatOption;

    public Builder formatOption(Format.FormatOptions formatOption) {
      this.formatOption = formatOption;
      return this;
    }

    /** Call the private constructor. */
    public UFFormatOptionConfig build() {
      return new UFFormatOptionConfig(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
