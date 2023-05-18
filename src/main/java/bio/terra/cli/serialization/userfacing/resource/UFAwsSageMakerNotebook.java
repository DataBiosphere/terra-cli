package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

/**
 * External representation of a workspace AWS SageMaker Notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsSageMakerNotebook} class for a notebook's internal representation.
 */
@JsonDeserialize(builder = UFAwsSageMakerNotebook.Builder.class)
public class UFAwsSageMakerNotebook extends UFResource {
  private static final Logger logger = LoggerFactory.getLogger(UFAwsSageMakerNotebook.class);

  public final String instanceName;
  public final String instanceType;
  public final String instanceStatus;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsSageMakerNotebook(AwsSageMakerNotebook internalObj) {
    super(internalObj);
    this.instanceName = internalObj.getInstanceName();
    this.instanceType = internalObj.getInstanceType();

    NotebookInstanceStatus notebookStatus = NotebookInstanceStatus.UNKNOWN_TO_SDK_VERSION;
    try {
      notebookStatus =
          WorkspaceManagerServiceAws.fromContext()
              .getSageMakerNotebookInstanceStatus(
                  internalObj,
                  WorkspaceManagerServiceAws.getSageMakerClient(
                      WorkspaceManagerServiceAws.fromContext()
                          .getControlledAwsSageMakerNotebookCredential(
                              Context.requireWorkspace().getUuid(), internalObj.getId()),
                      internalObj.getRegion()));
    } catch (Exception e) {
      logger.error(e.toString());
    }
    this.instanceStatus =
        (notebookStatus != NotebookInstanceStatus.UNKNOWN_TO_SDK_VERSION)
            ? notebookStatus.toString()
            : null;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsSageMakerNotebook(Builder builder) {
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
    OUT.println(prefix + "SageMaker Notebook instance name:   " + instanceName);
    OUT.println(
        prefix
            + "SageMaker Notebook instance type:   "
            + (instanceType == null ? "(unknown)" : instanceType));
    OUT.println(
        prefix
            + "SageMaker Notebook instance status: "
            + (instanceStatus == null ? "(unavailable)" : instanceStatus));
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
    public UFAwsSageMakerNotebook build() {
      return new UFAwsSageMakerNotebook(this);
    }
  }
}
