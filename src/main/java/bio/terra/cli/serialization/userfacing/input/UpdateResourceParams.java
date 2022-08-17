package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * Parameters for updating a workspace resource. This class is not currently user-facing, but could
 * be exposed as a command input format in the future. This class handles properties that are common
 * to all resource types. Resource-type specific classes include an instance of this class and any
 * additional properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateResourceParams {

  /** When null, the name of the resource should not be updated. */
  public final @Nullable String name;
  /** When null, the description of the resource should not be updated. */
  public final @Nullable String description;

  protected UpdateResourceParams(Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private @Nullable String name;
    private @Nullable String description;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder name(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder description(@Nullable String description) {
      this.description = description;
      return this;
    }

    /** Call the private constructor. */
    public UpdateResourceParams build() {
      return new UpdateResourceParams(this);
    }
  }
}
