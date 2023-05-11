package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsSagemakerNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSagemakerNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSagemakerNotebook;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsSagemakerNotebookResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS Sagemaker Notebook workspace resource. Instances of this class
 * are part of the current context or state.
 */
public class AwsSagemakerNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(AwsSagemakerNotebook.class);
  private final String instanceName;
  private final String instanceType;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsSagemakerNotebook(PDAwsSagemakerNotebook configFromDisk) {
    super(configFromDisk);
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName = configFromDisk.instanceName;
    this.instanceType = configFromDisk.instanceType;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsSagemakerNotebook(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName =
        wsmObject.getResourceAttributes().getAwsSagemakerNotebook().getInstanceName();
    this.instanceType =
        wsmObject.getResourceAttributes().getAwsSagemakerNotebook().getInstanceType();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsSagemakerNotebook(AwsSagemakerNotebookResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceName = wsmObject.getAttributes().getInstanceName();
    this.instanceType = wsmObject.getAttributes().getInstanceType();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsSagemakerNotebook serializeToCommand() {
    return new UFAwsSagemakerNotebook(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsSagemakerNotebook serializeToDisk() {
    return new PDAwsSagemakerNotebook(this);
  }

  /**
   * Add a AWS Sagemaker Notebook as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static AwsSagemakerNotebook addReferenced(CreateAwsSagemakerNotebookParams createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for AWS Sagemaker Notebook.");
  }

  /**
   * Create a AWS Sagemaker Notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsSagemakerNotebook createControlled(
      CreateAwsSagemakerNotebookParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AwsSagemakerNotebookResource createdResource =
        WorkspaceManagerServiceAws.fromContext()
            .createControlledAwsSagemakerNotebook(
                Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS Sagemaker Notebook: {}", createdResource);

    return new AwsSagemakerNotebook(createdResource);
  }

  /**
   * Delete a AWS Sagemaker Notebook referenced resource in the workspace. Currently unsupported.
   */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AWS Sagemaker Notebook.");
  }

  /** Delete a AWS Sagemaker Notebook controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerServiceAws.fromContext()
        .deleteControlledAwsSagemakerNotebook(Context.requireWorkspace().getUuid(), id);
  }

  /**
   * Resolve a AWS Sagemaker Notebook resource to its cloud identifier.
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
        .getAwsSagemakerNotebookCredential(
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
}
