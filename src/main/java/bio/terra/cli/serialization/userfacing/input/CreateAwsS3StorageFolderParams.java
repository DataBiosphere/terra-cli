package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a AWS bucket workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateAwsS3StorageFolderParams.Builder.class)
public class CreateAwsS3StorageFolderParams {
  public final CreateResourceParams resourceFields;
  public final String folderName;
  public final String region;

  protected CreateAwsS3StorageFolderParams(CreateAwsS3StorageFolderParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.folderName = builder.folderName;
    this.region = builder.region;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String folderName;
    private String region;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder folderName(String folderName) {
      this.folderName = folderName;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    /** Call the private constructor. */
    public CreateAwsS3StorageFolderParams build() {
      return new CreateAwsS3StorageFolderParams(this);
    }
  }
}
