package bio.terra.cli.serialization.userfacing.inputs;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a workspace resource. This class is not currently user-facing, but could
 * be exposed as a command input format in the future. This class handles properties that are common
 * to all resource types. Sub-classes include additional resource-type specific properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateResourceParams {
  public final String name;
  public final String description;

  protected UpdateResourceParams(Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String description;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /** Call the private constructor. */
    public UpdateResourceParams build() {
      return new UpdateResourceParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
