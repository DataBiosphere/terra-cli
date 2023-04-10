package bio.terra.cli.serialization.userfacing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the current server & workspace status for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFMount.Builder.class)
public class UFMount {

  public UFMount() {}

  /** Constructor for Jackson deserialization during testing. */
  private UFMount(Builder builder) {}

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Call the private constructor. */
    public UFMount build() {
      return new UFMount(this);
    }
  }
}
