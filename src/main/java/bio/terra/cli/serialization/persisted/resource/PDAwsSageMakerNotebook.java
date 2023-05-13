package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace AWS SageMaker Notebook resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link AwsSageMakerNotebook} class for a notebook's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAwsSageMakerNotebook.Builder.class)
public class PDAwsSageMakerNotebook extends PDResource {
  public final String instanceName;
  public final String instanceType;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsSageMakerNotebook(AwsSageMakerNotebook internalObj) {
    super(internalObj);
    this.instanceName = internalObj.getInstanceName();
    this.instanceType = internalObj.getInstanceType();
  }

  private PDAwsSageMakerNotebook(Builder builder) {
    super(builder);
    this.instanceName = builder.instanceName;
    this.instanceType = builder.instanceType;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsSageMakerNotebook deserializeToInternal() {
    return new AwsSageMakerNotebook(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String instanceName;
    private String instanceType;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder instanceName(String instanceName) {
      this.instanceName = instanceName;
      return this;
    }

    public Builder instanceType(String instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    /** Call the private constructor. */
    public PDAwsSageMakerNotebook build() {
      return new PDAwsSageMakerNotebook(this);
    }
  }
}
