package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a AWS bucket workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledAwsBucketParams {
  public final UpdateResourceParams resourceFields;
  public final AwsBucketLifecycle lifecycle;
  public final AwsBucketDefaultStorageClass defaultStorageClass;
  public final CloningInstructionsEnum cloningInstructions;

  protected UpdateControlledAwsBucketParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
    this.cloningInstructions = builder.cloningInstructions;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private AwsBucketLifecycle lifecycle;
    private AwsBucketDefaultStorageClass defaultStorageClass;
    private CloningInstructionsEnum cloningInstructions;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledAwsBucketParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledAwsBucketParams.Builder lifecycle(AwsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public UpdateControlledAwsBucketParams.Builder defaultStorageClass(
        AwsBucketDefaultStorageClass defaultStorageClass) {
      this.defaultStorageClass = defaultStorageClass;
      return this;
    }

    public UpdateControlledAwsBucketParams.Builder cloningInstructions(
        CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledAwsBucketParams build() {
      return new UpdateControlledAwsBucketParams(this);
    }
  }
}
