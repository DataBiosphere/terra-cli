package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating an AWS notebook workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateAwsNotebookParams.Builder.class)
public class CreateAwsNotebookParams {
  public final CreateResourceParams resourceFields;
  public final String instanceId;
  public final String location;
  public final String instanceType;

  protected CreateAwsNotebookParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
    this.instanceType = builder.instanceType;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String instanceId;
    private String location;
    private String instanceType;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder instanceType(String instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    /** Call the private constructor. */
    public CreateAwsNotebookParams build() {
      return new CreateAwsNotebookParams(this);
    }
  }
}
