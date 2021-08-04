package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery dataset workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateBqDatasetParams {
  public final UpdateResourceParams resourceFields;
  public final Integer partitionExpirationTime;
  public final Integer tableExpirationTime;

  protected UpdateBqDatasetParams(UpdateBqDatasetParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.partitionExpirationTime = builder.partitionExpirationTime;
    this.tableExpirationTime = builder.tableExpirationTime;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private Integer partitionExpirationTime;
    private Integer tableExpirationTime;

    public UpdateBqDatasetParams.Builder resourceFields(UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateBqDatasetParams.Builder partitionExpirationTime(Integer partitionExpirationTime) {
      this.partitionExpirationTime = partitionExpirationTime;
      return this;
    }

    public UpdateBqDatasetParams.Builder tableExpirationTime(Integer tableExpirationTime) {
      this.tableExpirationTime = tableExpirationTime;
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
