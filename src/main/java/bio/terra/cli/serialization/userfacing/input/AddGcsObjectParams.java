package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a GCS bucket object workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = AddGcsObjectParams.Builder.class)
public class AddGcsObjectParams {
  public final CreateResourceParams resourceFields;
  public final String bucketName;
  public final String objectName;

  protected AddGcsObjectParams(AddGcsObjectParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.objectName = builder.objectName;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String bucketName;
    private String objectName;

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

    public Builder objectName(String objectName) {
      this.objectName = objectName;
      return this;
    }

    /** Call the private constructor. */
    public AddGcsObjectParams build() {
      return new AddGcsObjectParams(this);
    }
  }
}
