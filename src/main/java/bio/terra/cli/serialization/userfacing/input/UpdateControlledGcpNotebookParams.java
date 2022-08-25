package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.GcpAiNotebookUpdateParameters;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a GCP Notebook workspace controlled resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledGcpNotebookParams {
  /** When null, the name of the resource should not be updated. */
  public final UpdateResourceParams resourceFields;

  public final GcpAiNotebookUpdateParameters notebookUpdateParameters;

  protected UpdateControlledGcpNotebookParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.notebookUpdateParameters = builder.notebookUpdateParameters;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    public GcpAiNotebookUpdateParameters notebookUpdateParameters;
    private UpdateResourceParams resourceFields;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledGcpNotebookParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledGcpNotebookParams.Builder notebookUpdateParameters(
        GcpAiNotebookUpdateParameters notebookUpdateParameters) {
      this.notebookUpdateParameters = notebookUpdateParameters;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledGcpNotebookParams build() {
      return new UpdateControlledGcpNotebookParams(this);
    }
  }
}
