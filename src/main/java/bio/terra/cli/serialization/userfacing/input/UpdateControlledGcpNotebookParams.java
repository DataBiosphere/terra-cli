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
public class UpdateControlledGcpNotebookParams {
  /** When null, the name of the resource should not be updated. */
  public final UpdateResourceParams resourceFields;

  protected UpdateControlledGcpNotebookParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;

    public UpdateControlledGcpNotebookParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledGcpNotebookParams build() {
      return new UpdateControlledGcpNotebookParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
