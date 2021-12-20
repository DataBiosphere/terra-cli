package bio.terra.cli.serialization.userfacing.input;

import bio.terra.cli.businessobject.resource.GcsObject;
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
  /**
   * When bucket name is null, it is not to be updated. Instead, get the bucket name from the {@code
   * originalResource} instead.
   */
  public final @Nullable String bucketName;
  /**
   * When object name is null, it is not to be updated. instead, get the object name from the {@code
   * originalResource} instead.
   */
  public final @Nullable String objectName;
  /**
   * WSM currently requires both bucket name and object name to be specified when updating the
   * referencing target. So when a user wants to update the reference to another object in the same
   * bucket and didn't specify the bucket name, we will fetch the original bucketName from {@code
   * originalResource}. Same when the user only specify new bucket name.
   */
  public final GcsObject originalResource;

  protected UpdateReferencedGcsObjectParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.objectName = builder.objectName;
    this.originalResource = builder.originalResource;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private @Nullable String bucketName;
    private @Nullable String objectName;
    private GcsObject originalResource;

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

    public UpdateReferencedGcsObjectParams.Builder originalResource(GcsObject originalResource) {
      this.originalResource = originalResource;
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
