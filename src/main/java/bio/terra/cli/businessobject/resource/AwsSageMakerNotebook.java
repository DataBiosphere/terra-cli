package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsSageMakerNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSageMakerNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSageMakerNotebook;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsSageMakerNotebookResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS SageMaker Notebook workspace resource. Instances of this class
 * are part of the current context or state.
 */
public class AwsSageMakerNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(AwsSageMakerNotebook.class);
  private final String instanceName;
  private final String instanceType;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsSageMakerNotebook(PDAwsSageMakerNotebook configFromDisk) {
    super(configFromDisk);
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName = configFromDisk.instanceName;
    this.instanceType = configFromDisk.instanceType;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsSageMakerNotebook(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName =
        wsmObject.getResourceAttributes().getAwsSageMakerNotebook().getInstanceName();
    this.instanceType =
        wsmObject.getResourceAttributes().getAwsSageMakerNotebook().getInstanceType();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsSageMakerNotebook(AwsSageMakerNotebookResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName = wsmObject.getAttributes().getInstanceName();
    this.instanceType = wsmObject.getAttributes().getInstanceType();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsSageMakerNotebook serializeToCommand() {
    return new UFAwsSageMakerNotebook(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsSageMakerNotebook serializeToDisk() {
    return new PDAwsSageMakerNotebook(this);
  }

  /**
   * Add a AWS SageMaker Notebook as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static AwsSageMakerNotebook addReferenced(CreateAwsSageMakerNotebookParams createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for AWS SageMaker Notebook.");
  }

  /**
   * Create a AWS SageMaker Notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsSageMakerNotebook createControlled(
      CreateAwsSageMakerNotebookParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AwsSageMakerNotebookResource createdResource =
        WorkspaceManagerServiceAws.fromContext()
            .createControlledAwsSageMakerNotebook(
                Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS SageMaker Notebook: {}", createdResource);

    return new AwsSageMakerNotebook(createdResource);
  }

  /**
   * Delete a AWS SageMaker Notebook referenced resource in the workspace. Currently unsupported.
   */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AWS SageMaker Notebook.");
  }

  /** Delete a AWS SageMaker Notebook controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerServiceAws.fromContext()
        .deleteControlledAwsSageMakerNotebook(Context.requireWorkspace().getUuid(), id);
  }

  /**
   * Resolve a AWS SageMaker Notebook resource to its cloud identifier.
   * https://[region].console.aws.amazon.com/sagemaker/home?region=[region]#/notebook-instances/[instance-name]
   */
  public String resolve() {
    return String.format(
        "https://%s.console.aws.amazon.com/sagemaker/home?region=%s#/notebook-instances/%s",
        region, region, instanceName);
  }

  public Object getCredentials(CredentialsAccessScope scope, int duration) {
    // call WSM to get credentials
    return WorkspaceManagerServiceAws.fromContext()
        .getControlledAwsSageMakerNotebookCredential(
            Context.requireWorkspace().getUuid(),
            id,
            scope == CredentialsAccessScope.READ_ONLY
                ? AwsCredentialAccessScope.READ_ONLY
                : AwsCredentialAccessScope.WRITE_READ,
            duration);
  }

  // ====================================================
  // Property getters.

  public String getInstanceName() {
    return instanceName;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public enum ProxyView {
    JUPYTER,
    JUPYTERLAB;

    @Override
    public String toString() {
      return (this == ProxyView.JUPYTERLAB) ? "lab" : "classic";
    }
  }
}
