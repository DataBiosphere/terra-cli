package bio.terra.cli.resources;

import bio.terra.cli.Context;
import bio.terra.cli.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateAiNotebook;
import bio.terra.cli.serialization.disk.resources.DiskAiNotebook;
import bio.terra.cli.service.GoogleAiNotebooks;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.GcpAiNotebookInstanceResource;
import bio.terra.workspace.model.ResourceDescription;
import com.google.api.services.notebooks.v1.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucket.class);

  private String projectId;
  private String instanceId;
  private String location;

  protected AiNotebook(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
  }

  /**
   * Add an AI Platform notebook as a referenced resource in the workspace. Currently unsupported.
   */
  public static AiNotebook addReferenced(CreateUpdateAiNotebook createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for AI Platform notebooks.");
  }

  /**
   * Create an AI notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AiNotebook createControlled(CreateUpdateAiNotebook createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpAiNotebookInstanceResource createdResource =
        new WorkspaceManagerService()
            .createControlledAiNotebookInstance(Context.requireWorkspace().getId(), createParams);
    logger.info("Created AI notebook: {}", createdResource);

    // convert the WSM object to a CLI object
    listAndSync();
    return new Builder(createdResource).build();
  }

  /** Delete an AI Platform notebook referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AI Platform notebooks.");
  }

  /** Delete an AI Platform notebook controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    new WorkspaceManagerService()
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

  /** Check whether a user can access the AI Platform notebook resource. Currently unsupported. */
  public boolean checkAccess(CheckAccessCredentials credentialsToUse) {
    throw new UserActionableException("Check access not supported for AI Platform notebooks.");
  }

  /** Query the cloud for information about the notebook VM. */
  public Instance getInstance() {
    InstanceName instanceName =
        InstanceName.builder()
            .projectId(projectId)
            .location(location)
            .instanceId(instanceId)
            .build();
    GoogleAiNotebooks notebooks = new GoogleAiNotebooks(Context.requireUser().getUserCredentials());
    return notebooks.get(instanceName);
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

  /**
   * Builder class to help construct an immutable Resource object with lots of properties.
   * Sub-classes extend this with resource type-specific properties.
   */
  public static class Builder extends Resource.Builder {
    private String projectId;
    private String instanceId;
    private String location;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
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

    /** Method that returns the resource type. Should be hard-coded in sub-classes. */
    public ResourceType getResourceType() {
      return ResourceType.AI_NOTEBOOK;
    }

    /** Call the sub-class constructor. */
    public AiNotebook build() {
      return new AiNotebook(this);
    }

    /**
     * Populate this Builder object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to GCS buckets only.
     */
    public Builder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getProjectId();
      this.instanceId =
          wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getInstanceId();
      this.location = wsmObject.getResourceAttributes().getGcpAiNotebookInstance().getLocation();
    }

    /** Populate this Builder object with properties from the WSM GcpGcsBucketResource object. */
    public Builder(GcpAiNotebookInstanceResource wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getAttributes().getProjectId();
      this.instanceId = wsmObject.getAttributes().getInstanceId();
      this.location = wsmObject.getAttributes().getLocation();
    }

    /**
     * Populate this Builder object with properties from the on-disk object. This method handles the
     * fields that apply to all resource types.
     */
    public Builder(DiskAiNotebook configFromDisk) {
      super(configFromDisk);
      this.projectId = configFromDisk.projectId;
      this.instanceId = configFromDisk.instanceId;
      this.location = configFromDisk.location;
    }
  }
}
