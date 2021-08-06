package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery dataset workspace controlled resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateBqDatasetParams {
  public final UpdateResourceParams resourceFields;
  public final Integer defaultPartitionLifetime;
  public final Integer defaultTableLifetime;

  protected UpdateBqDatasetParams(UpdateBqDatasetParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.defaultPartitionLifetime = builder.defaultPartitionLifetime;
    this.defaultTableLifetime = builder.defaultTableLifetime;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private Integer defaultPartitionLifetime;
    private Integer defaultTableLifetime;

    public UpdateBqDatasetParams.Builder resourceFields(UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder defaultPartitionLifetime(Integer defaultPartitionLifetime) {
      this.defaultPartitionLifetime = defaultPartitionLifetime;
      return this;
    }

    public Builder defaultTableLifetime(Integer defaultTableLifetime) {
      this.defaultTableLifetime = defaultTableLifetime;
      return this;
    }

    /** Call the private constructor. */
    public UpdateBqDatasetParams build() {
      return new UpdateBqDatasetParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
