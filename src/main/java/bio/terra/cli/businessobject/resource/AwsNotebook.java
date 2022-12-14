package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AwsSageMakerNotebookResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS notebook workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class AwsNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(AwsNotebook.class);
  private final String awsAccountNumber;
  private final String landingZoneId;
  private final String instanceId;
  private final String location;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsNotebook(PDAwsNotebook configFromDisk) {
    super(configFromDisk);
    this.awsAccountNumber = configFromDisk.awsAccountNumber;
    this.landingZoneId = configFromDisk.landingZoneId;
    this.instanceId = configFromDisk.instanceId;
    this.location = configFromDisk.location;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsNotebook(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.awsAccountNumber = wsmObject.getMetadata().getResourceId().toString(); // TODO-Dex
    this.landingZoneId = wsmObject.getMetadata().getWorkspaceId().toString(); // TODO-Dex
    this.instanceId = wsmObject.getMetadata().getResourceId().toString();
    this.location = wsmObject.getResourceAttributes().getAwsSagemakerNotebook().getRegion();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsNotebook(AwsSageMakerNotebookResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_SAGEMAKER_NOTEBOOK;
    this.awsAccountNumber = wsmObject.getMetadata().getResourceId().toString(); // TODO-Dex
    this.landingZoneId = wsmObject.getMetadata().getWorkspaceId().toString(); // TODO-Dex
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
    Context.requireWorkspace().listResourcesAndSync();
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

  /**
   * Resolve a AWS notebook resource to its cloud identifier. Return the instance name
   * projects/[project_id]/locations/[location]/instances/[instanceId].
   *
   * @return full name of the instance
   */
  public String resolve() { // TODO(TERRA-197)
    return String.format("locations/%s/instances/%s", location, instanceId);
  }

  /**
   * Query the cloud for information about the notebook VM. * / public
   * Optional<AwsNotebookInstanceName> getInstance() { Optional<AwsNotebook> awsNotebook =
   * getResource(instanceId); if (awsNotebook.isPresent()) { AwsNotebookInstanceName instanceName =
   * AwsNotebookInstanceName.builder().instanceId(instanceId).location(location).build();
   * AmazonNotebooks notebooks = new AmazonNotebooks( WorkspaceManagerService.fromContext()
   * .getAwsSageMakerNotebookCredential( Context.requireWorkspace().getUuid(),
   * awsNotebook.get().getId())); // return Optional.of(notebooks.get(instanceName)); } return
   * Optional.empty(); }
   */

  /** Find the resource by instance id. */
  public Optional<AwsNotebook> getResource(String instanceId) {
    Resource resource = Context.requireWorkspace().getResource(instanceId);

    if (resource.getResourceType().equals(Resource.Type.AWS_SAGEMAKER_NOTEBOOK)) {
      return Optional.of((AwsNotebook) resource);
    } else {
      logger.error(
          "Specified resource is not a SageMaker notebook, but " + resource.getResourceType());
      return Optional.empty();
    }
  }

  // ====================================================
  // Property getters.

  public String getAwsAccountNumber() {
    return awsAccountNumber;
  }

  public String getLandingZoneId() {
    return landingZoneId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getLocation() {
    return location;
  }
}
