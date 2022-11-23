package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a AWS Notebook workspace controlled resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledAwsNotebookParams {
  /** When null, the name of the resource should not be updated. */
  public final UpdateResourceParams resourceFields;
  // public final AwsNotebookUpdateParameters notebookUpdateParameters;

  protected UpdateControlledAwsNotebookParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    // this.notebookUpdateParameters = builder.notebookUpdateParameters;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    // public AwsNotebookUpdateParameters notebookUpdateParameters;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledAwsNotebookParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledAwsNotebookParams.Builder notebookUpdateParameters() {
      //  GcpAiNotebookUpdateParameters notebookUpdateParameters) {
      // this.notebookUpdateParameters = notebookUpdateParameters;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledAwsNotebookParams build() {
      return new UpdateControlledAwsNotebookParams(this);
    }
  }
}
