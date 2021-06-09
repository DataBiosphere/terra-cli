package bio.terra.cli.serialization.userfacing.inputs;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating/updating a BQ dataset workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateUpdateBqDataset.Builder.class)
public class CreateUpdateBqDataset {
  public final CreateUpdateResource resourceFields;
  public final String projectId;
  public final String datasetId;
  public final String location;

  protected CreateUpdateBqDataset(CreateUpdateBqDataset.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.location = builder.location;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateUpdateResource resourceFields;
    private String projectId;
    private String datasetId;
    private String location;

    public Builder resourceFields(CreateUpdateResource resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public CreateUpdateBqDataset build() {
      return new CreateUpdateBqDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
