package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a BQ DataTable workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateBqDataTableParams.Builder.class)
public class CreateBqDataTableParams {
  public final CreateResourceParams resourceFields;
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;
  public final String location;

  protected CreateBqDataTableParams(CreateBqDataTableParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
    this.location = builder.location;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String projectId;
    private String datasetId;
    private String dataTableId;
    private String location;

    public Builder resourceFields(CreateResourceParams resourceFields) {
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

    public Builder dataTableId(String dataTableId) {
      this.dataTableId = dataTableId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public CreateBqDataTableParams build() {
      return new CreateBqDataTableParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
