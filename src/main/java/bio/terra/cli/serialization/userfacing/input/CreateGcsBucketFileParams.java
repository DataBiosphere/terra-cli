package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a GCS bucket file workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateGcsBucketFileParams.Builder.class)
public class CreateGcsBucketFileParams {
  public final CreateResourceParams resourceFields;
  public final String bucketName;
  public final String bucketFileName;

  protected CreateGcsBucketFileParams(CreateGcsBucketFileParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.bucketFileName = builder.bucketFileName;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String bucketName;
    private String bucketFileName;

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder bucketFileName(String bucketFileName) {
      this.bucketFileName = bucketFileName;
      return this;
    }

    /** Call the private constructor. */
    public CreateGcsBucketFileParams build() {
      return new CreateGcsBucketFileParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
