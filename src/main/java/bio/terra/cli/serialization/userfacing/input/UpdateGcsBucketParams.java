package bio.terra.cli.serialization.userfacing.input;

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
public class UpdateGcsBucketParams {
  public final UpdateResourceParams resourceFields;
  public final GcsBucketLifecycle lifecycle;
  public final GcpGcsBucketDefaultStorageClass defaultStorageClass;

  protected UpdateGcsBucketParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private GcsBucketLifecycle lifecycle;
    private GcpGcsBucketDefaultStorageClass defaultStorageClass;

    public UpdateGcsBucketParams.Builder resourceFields(UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateGcsBucketParams.Builder lifecycle(GcsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public UpdateGcsBucketParams.Builder defaultStorageClass(
        GcpGcsBucketDefaultStorageClass defaultStorageClass) {
      this.defaultStorageClass = defaultStorageClass;
      return this;
    }

    /** Call the private constructor. */
    public UpdateGcsBucketParams build() {
      return new UpdateGcsBucketParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
