package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsSagemakerNotebook;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace AWS Sagemaker Notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsSagemakerNotebook} class for a notebook's internal representation.
 */
@JsonDeserialize(builder = UFAwsSagemakerNotebook.Builder.class)
public class UFAwsSagemakerNotebook extends UFResource {
  public final String instanceName;
  public final String instanceType;
  public final String instanceStatus;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsSagemakerNotebook(AwsSagemakerNotebook internalObj) {
    super(internalObj);
    this.instanceName = internalObj.getInstanceName();
    this.instanceType = internalObj.getInstanceType();
    this.instanceStatus = null;
    /*this.instanceStatus =
    AwsNotebook.getInstanceStatus(location, instanceId)
            .orElse(NotebookInstanceStatus.UNKNOWN_TO_SDK_VERSION)
            .toString();*/
    // TODO(TERRA-320) fill status
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsSagemakerNotebook(Builder builder) {
    super(builder);
    this.instanceName = builder.instanceName;
    this.instanceType = builder.instanceType;
    this.instanceStatus = builder.instanceStatus;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Sagemaker Notebook instance name:   " + instanceName);
    OUT.println(
        prefix
            + "Sagemaker Notebook instance type:   "
            + (instanceType == null ? "(unknown)" : instanceType));
    OUT.println(
        prefix
            + "Sagemaker Notebook instance status: "
            + (instanceStatus == null ? "(unknown)" : instanceStatus));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String instanceName;
    private String instanceType;
    private String instanceStatus;

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

    public Builder instanceStatus(String instanceStatus) {
      this.instanceStatus = instanceStatus;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsSagemakerNotebook build() {
      return new UFAwsSagemakerNotebook(this);
    }
  }
}
