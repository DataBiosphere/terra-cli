package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * Parameters for creating a BQ dataset workspace controlled resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateBqDatasetParams.Builder.class)
public class CreateBqDatasetParams {
  public final CreateResourceParams resourceFields;
  public final String projectId;
  public final @Nullable String datasetId;
  public final String location;
  public final Integer defaultPartitionLifetimeSeconds;
  public final Integer defaultTableLifetimeSeconds;

  protected CreateBqDatasetParams(CreateBqDatasetParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.location = builder.location;
    this.defaultPartitionLifetimeSeconds = builder.defaultPartitionLifetimeSeconds;
    this.defaultTableLifetimeSeconds = builder.defaultTableLifetimeSeconds;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String projectId;
    private @Nullable String datasetId;
    private String location;
    private Integer defaultPartitionLifetimeSeconds;
    private Integer defaultTableLifetimeSeconds;

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(@Nullable String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder defaultPartitionLifetimeSeconds(Integer defaultPartitionLifetimeSeconds) {
      this.defaultPartitionLifetimeSeconds = defaultPartitionLifetimeSeconds;
      return this;
    }

    public Builder defaultTableLifetimeSeconds(Integer defaultTableLifetimeSeconds) {
      this.defaultTableLifetimeSeconds = defaultTableLifetimeSeconds;
      return this;
    }

    /** Call the private constructor. */
    public CreateBqDatasetParams build() {
      return new CreateBqDatasetParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
