package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * Parameters for updating a referenced GCS bucket workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedGcsObjectParams {
  public final UpdateResourceParams resourceFields;
  public final @Nullable String bucketName;
  public final @Nullable String objectName;

  protected UpdateReferencedGcsObjectParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.objectName = builder.objectName;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private @Nullable String bucketName;
    private @Nullable String objectName;

    public UpdateReferencedGcsObjectParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedGcsObjectParams.Builder bucketName(@Nullable String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public UpdateReferencedGcsObjectParams.Builder objectName(@Nullable String objectName) {
      this.objectName = objectName;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedGcsObjectParams build() {
      return new UpdateReferencedGcsObjectParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
