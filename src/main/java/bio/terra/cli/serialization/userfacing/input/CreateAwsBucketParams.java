package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.AwsBucketDefaultStorageClass;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a AWS bucket workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateAwsBucketParams.Builder.class)
public class CreateAwsBucketParams {
  public final CreateResourceParams resourceFields;
  public final String bucketName;
  public final AwsBucketLifecycle lifecycle;
  public final AwsBucketDefaultStorageClass defaultStorageClass;
  public final String location;

  protected CreateAwsBucketParams(CreateAwsBucketParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
    this.location = builder.location;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String bucketName;
    private AwsBucketLifecycle lifecycle;
    private AwsBucketDefaultStorageClass defaultStorageClass;
    private String location;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder lifecycle(AwsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public Builder defaultStorageClass(AwsBucketDefaultStorageClass defaultStorageClass) {
      this.defaultStorageClass = defaultStorageClass;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public CreateAwsBucketParams build() {
      return new CreateAwsBucketParams(this);
    }
  }
}
