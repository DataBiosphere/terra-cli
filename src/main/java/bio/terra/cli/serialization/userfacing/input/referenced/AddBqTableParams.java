package bio.terra.cli.serialization.userfacing.input.referenced;

import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a BQ DataTable workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = AddBqTableParams.Builder.class)
public class AddBqTableParams {
  public final CreateResourceParams resourceFields;
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;

  protected AddBqTableParams(AddBqTableParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String projectId;
    private String datasetId;
    private String dataTableId;

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

    /** Call the private constructor. */
    public AddBqTableParams build() {
      return new AddBqTableParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
