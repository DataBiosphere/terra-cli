package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a GCS bucket workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledAwsBucketParams {
  public final UpdateResourceParams resourceFields;
  public final GcsBucketLifecycle lifecycle;
  public final GcpGcsBucketDefaultStorageClass defaultStorageClass;
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
    private GcsBucketLifecycle lifecycle;
    private GcpGcsBucketDefaultStorageClass defaultStorageClass;
    private CloningInstructionsEnum cloningInstructions;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledAwsBucketParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledAwsBucketParams.Builder lifecycle(GcsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public UpdateControlledAwsBucketParams.Builder defaultStorageClass(
        GcpGcsBucketDefaultStorageClass defaultStorageClass) {
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
