package bio.terra.cli.serialization.userfacing.input.referenced;

import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery table workspace referenced resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedGcsBucketParams {
  private final UpdateResourceParams resourceParams;
  private final String bucketName;

  protected UpdateReferencedGcsBucketParams(UpdateReferencedGcsBucketParams.Builder builder) {
    this.resourceParams = builder.resourceFields;
    this.bucketName = builder.bucketName;
  }

  public UpdateResourceParams getResourceParams() {
    return resourceParams;
  }

  public String getBucketName() {
    return bucketName;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateResourceParams resourceFields;
    private String bucketName;

    public UpdateReferencedGcsBucketParams.Builder resourceParams(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedGcsBucketParams.Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedGcsBucketParams build() {
      return new UpdateReferencedGcsBucketParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
