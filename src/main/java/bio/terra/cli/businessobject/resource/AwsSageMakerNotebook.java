package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsSageMakerNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSageMakerNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSageMakerNotebook;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.cli.utils.AwsConfiguration;
import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsSageMakerNotebookResource;
import bio.terra.workspace.model.ResourceDescription;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.utils.URIBuilder;

/**
 * Internal representation of a AWS SageMaker Notebook workspace resource. Instances of this class
 * are part of the current context or state.
 */
public class AwsSageMakerNotebook extends Resource {
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

  /** Create a AWS SageMaker Notebook as a controlled resource in the workspace. */
  public static void createControlled(CreateAwsSageMakerNotebookParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    Workspace workspace = Context.requireWorkspace();

    // call WSM to create the resource
    WorkspaceManagerServiceAws.fromContext()
        .createControlledAwsSageMakerNotebook(workspace.getUuid(), createParams);

    AwsConfiguration awsConfiguration = AwsConfiguration.loadFromDisk(workspace.getUuid());
    awsConfiguration.addResource(
        createParams.resourceFields.name, createParams.region, Type.AWS_SAGEMAKER_NOTEBOOK);
    awsConfiguration.storeToDisk();
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
    Workspace workspace = Context.requireWorkspace();

    // call WSM to delete the resource
    WorkspaceManagerServiceAws.fromContext()
        .deleteControlledAwsSageMakerNotebook(Context.requireWorkspace().getUuid(), id);

    AwsConfiguration awsConfiguration = AwsConfiguration.loadFromDisk(workspace.getUuid());
    awsConfiguration.removeResource(getName());
    awsConfiguration.storeToDisk();
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

  public URL getConsoleUrl(CredentialsAccessScope scope, int duration) {
    try {
      URL destinationUrl =
          new URIBuilder()
              .setScheme("https")
              .setHost(String.format("%s.console.aws.amazon.com", region))
              .setPath("sagemaker/home")
              .setParameter("region", region)
              .setFragment(String.format("/notebook-instances/%s", instanceName))
              .build()
              .toURL();
      return WorkspaceManagerServiceAws.createConsoleUrl(
          (AwsCredential) getCredentials(scope, duration), duration, destinationUrl);

    } catch (URISyntaxException | IOException e) {
      throw new SystemException("Failed to create destination URL.", e);
    }
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

    public String toParam() {
      return (this == ProxyView.JUPYTERLAB) ? "lab" : "classic";
    }
  }
}
