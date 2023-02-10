package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.cloud.aws.SageMakerNotebooksCow;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsSageMakerNotebookResource;
import bio.terra.workspace.model.AwsSageMakerProxyUrlView;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

/**
 * Internal representation of a AWS notebook workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class AwsNotebook extends Resource {
  private static final String AWS_NOTEBOOK_URL_PREFIX = "https://";
  private static final Logger logger = LoggerFactory.getLogger(AwsNotebook.class);
  private final String instanceId;
  private final String location;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsNotebook(PDAwsNotebook configFromDisk) {
    super(configFromDisk);
    this.instanceId = configFromDisk.instanceId;
    this.location = configFromDisk.location;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsNotebook(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceId = wsmObject.getMetadata().getName();
    this.location = wsmObject.getResourceAttributes().getAwsSagemakerNotebook().getRegion();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsNotebook(AwsSageMakerNotebookResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.instanceId = wsmObject.getAttributes().getInstanceId();
    this.location = wsmObject.getAttributes().getRegion();
  }

  /** Add a AWS notebook as a referenced resource in the workspace. Currently unsupported. */
  public static AwsNotebook addReferenced(CreateAwsNotebookParams createParams) {
    throw new UserActionableException("Referenced resources not supported for AWS notebooks.");
  }

  /**
   * Create a AWS notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsNotebook createControlled(CreateAwsNotebookParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AwsSageMakerNotebookResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledAwsNotebookInstance(
                Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS notebook: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResources();
    return new AwsNotebook(createdResource);
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsNotebook serializeToCommand() {
    return new UFAwsNotebook(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsNotebook serializeToDisk() {
    return new PDAwsNotebook(this);
  }

  public void updateControlled(UpdateControlledAwsNotebookParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateResourceName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateControlledAwsNotebook(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
  }

  /** Delete a AWS notebook referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException("Referenced resources not supported for AWS notebooks.");
  }

  /** Delete a AWS notebook controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledAwsNotebookInstance(Context.requireWorkspace().getUuid(), id);
  }

  /** Resolve a AWS notebook resource to its cloud identifier. */
  public String resolve() {
    return resolve(true);
  }

  /**
   * Resolve a AWS notebook resource to its cloud identifier. Optionally include the 's3://' prefix.
   */
  public String resolve(boolean includeUrlPrefix) {
    return resolve(location, instanceId, includeUrlPrefix);
  }

  /**
   * Resolve a AWS notebook resource to its cloud identifier. Return the instance name
   * https://[location].console.aws.amazon.com/sagemaker/home?region=[location]#/notebook-instances/[instance-id]
   *
   * @return full name of the instance
   */
  public static String resolve(String location, String instanceId, boolean includeUrlPrefix) {
    String resolvedPath =
        String.format(
            "%s.console.aws.amazon.com/sagemaker/home?region=%s#/notebook-instances/%s",
            location, location, instanceId);
    return includeUrlPrefix ? AWS_NOTEBOOK_URL_PREFIX + resolvedPath : resolvedPath;
  }

  public static NotebookInstanceStatus getStatus(String location, String instanceId) {
    Workspace workspace = Context.requireWorkspace();
    return SageMakerNotebooksCow.create(
            WorkspaceManagerService.fromContext()
                .getAwsSageMakerNotebookCredential(
                    workspace.getUuid(),
                    workspace.getResource(instanceId).getId(),
                    AwsCredentialAccessScope.READ_ONLY,
                    WorkspaceManagerService.AWS_CREDENTIAL_EXPIRATION_SECONDS_DEFAULT),
            location)
        .get(instanceId)
        .notebookInstanceStatus();
  }

  public static Optional<String> getProxyUri(
      String instanceId, AwsSageMakerProxyUrlView proxyUrlView, boolean rethrowException) {
    Workspace workspace = Context.requireWorkspace();
    try {
      return Optional.ofNullable(
          WorkspaceManagerService.fromContext()
              .getAwsSageMakerProxyUrl(
                  workspace.getUuid(),
                  workspace.getResource(instanceId).getId(),
                  proxyUrlView,
                  WorkspaceManagerService.AWS_PROXY_URL_EXPIRATION_SECONDS_DEFAULT)
              .getUrl());
    } catch (SystemException e) {
      if (rethrowException) {
        throw e;
      }
      // else: do not rethrow exception
    }
    return Optional.empty();
  }

  // ====================================================
  // Property getters.

  public String getInstanceId() {
    return instanceId;
  }

  public String getLocation() {
    return location;
  }
}
