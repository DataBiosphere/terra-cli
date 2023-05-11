package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a AWS Sagemaker Notebook workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateAwsSagemakerNotebookParams.Builder.class)
public class CreateAwsSagemakerNotebookParams {
  public final CreateResourceParams resourceFields;
  public final String instanceName;
  public final String instanceType;
  public final String region;

  protected CreateAwsSagemakerNotebookParams(CreateAwsSagemakerNotebookParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.instanceName = builder.instanceName;
    this.instanceType = builder.instanceType;
    this.region = builder.region;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String instanceName;
    private String instanceType;
    private String region;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder instanceName(String instanceName) {
      this.instanceName = instanceName;
      return this;
    }

    public Builder instanceType(String instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    /** Call the private constructor. */
    public CreateAwsSagemakerNotebookParams build() {
      return new CreateAwsSagemakerNotebookParams(this);
    }
  }
}
