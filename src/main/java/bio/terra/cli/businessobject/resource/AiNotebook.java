package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAiNotebook;
import bio.terra.cli.serialization.userfacing.input.CreateAiNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAiNotebook;
import bio.terra.cli.service.GoogleAiNotebooks;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.GcpAiNotebookInstanceResource;
import bio.terra.workspace.model.ResourceDescription;
import com.google.api.services.notebooks.v1.model.Instance;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of an AI notebook workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class AiNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(AiNotebook.class);

  private String projectId;
  private String instanceId;
  private String location;

  /** Deserialize an instance of the disk format to the internal object. */
  public AiNotebook(PDAiNotebook configFromDisk) {
    super(configFromDisk);
    this.projectId = configFromDisk.projectId;
    this.instanceId = configFromDisk.instanceId;
    this.location = configFromDisk.location;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AiNotebook(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AI_NOTEBOOK;
    this.projectId = wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getProjectId();
    this.instanceId = wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getInstanceId();
    this.location = wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getLocation();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AiNotebook(GcpAiNotebookInstanceResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AI_NOTEBOOK;
    this.projectId = wsmObject.getAttributes().getProjectId();
    this.instanceId = wsmObject.getAttributes().getInstanceId();
    this.location = wsmObject.getAttributes().getLocation();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAiNotebook serializeToCommand() {
    return new UFAiNotebook(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAiNotebook serializeToDisk() {
    return new PDAiNotebook(this);
  }

  /**
   * Add an AI Platform notebook as a referenced resource in the workspace. Currently unsupported.
   */
  public static AiNotebook addReferenced(CreateAiNotebookParams createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for AI Platform notebooks.");
  }

  /**
   * Create an AI notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AiNotebook createControlled(CreateAiNotebookParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to create the resource
    GcpAiNotebookInstanceResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledAiNotebookInstance(Context.requireWorkspace().getId(), createParams);
    logger.info("Created AI notebook: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new AiNotebook(createdResource);
  }

  /** Delete an AI Platform notebook referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AI Platform notebooks.");
  }

  /** Delete an AI Platform notebook controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledAiNotebookInstance(Context.requireWorkspace().getId(), id);
  }

  /**
   * Resolve an AI Platform notebook resource to its cloud identifier. Return the instance name
   * projects/[project_id]/locations/[location]/instances/[instanceId].
   *
   * @return full name of the instance
   */
  public String resolve() {
    return String.format("projects/%s/locations/%s/instances/%s", projectId, location, instanceId);
  }

  /** Query the cloud for information about the notebook VM. */
  public Optional<Instance> getInstance() {
    InstanceName instanceName =
        InstanceName.builder()
            .projectId(projectId)
            .location(location)
            .instanceId(instanceId)
            .build();
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(Context.requireUser().getPetSACredentials());
    try {
      return Optional.of(notebooks.get(instanceName));
    } catch (Exception ex) {
      logger.error("Caught exception looking up notebook instance", ex);
      return Optional.empty();
    }
  }

  // ====================================================
  // Property getters.

  public String getProjectId() {
    return projectId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getLocation() {
    return location;
  }
}
