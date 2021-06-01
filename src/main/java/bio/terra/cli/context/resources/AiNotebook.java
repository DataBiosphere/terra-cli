package bio.terra.cli.context.resources;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.GoogleAiNotebooks;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.GcpAiNotebookInstanceResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.notebooks.v1.model.Instance;
import java.io.PrintStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonDeserialize(builder = AiNotebook.AiNotebookBuilder.class)
public class AiNotebook extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(AiNotebook.class);

  public final String projectId;
  public final String instanceId;
  public final String location;
  public final String machineType;
  public final String postStartupScript;
  public final Map<String, String> metadata;
  public final String vmImageProject;
  public final String vmImageFamily;
  public final String vmImageName;
  public final String containerRepository;
  public final String containerTag;
  public final String acceleratorType;
  public final Long acceleratorCoreCount;
  public final Boolean installGpuDriver;
  public final String customGpuDriverPath;
  public final Long bootDiskSizeGb;
  public final String bootDiskType;
  public final Long dataDiskSizeGb;
  public final String dataDiskType;

  // only include this property when serializing (e.g. writing out to the user), not when
  // deserializing (e.g. reading in from the context file)
  @JsonIgnore private Instance instance;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public void setInstance(Instance instance) {
    this.instance = instance;
  }

  public AiNotebook(AiNotebookBuilder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
    this.machineType = builder.machineType;
    this.postStartupScript = builder.postStartupScript;
    this.metadata = builder.metadata;
    this.vmImageProject = builder.vmImageProject;
    this.vmImageFamily = builder.vmImageFamily;
    this.vmImageName = builder.vmImageName;
    this.containerRepository = builder.containerRepository;
    this.containerTag = builder.containerTag;
    this.acceleratorType = builder.acceleratorType;
    this.acceleratorCoreCount = builder.acceleratorCoreCount;
    this.installGpuDriver = builder.installGpuDriver;
    this.customGpuDriverPath = builder.customGpuDriverPath;
    this.bootDiskSizeGb = builder.bootDiskSizeGb;
    this.bootDiskType = builder.bootDiskType;
    this.dataDiskSizeGb = builder.dataDiskSizeGb;
    this.dataDiskType = builder.dataDiskType;
  }

  /**
   * Add an AI Platform notebook as a referenced resource in the workspace. Currently unsupported.
   */
  protected AiNotebook addReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AI Platform notebooks.");
  }

  /**
   * Create an AI Platform notebook as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  protected AiNotebook createControlled() {
    // call WSM to create the resource
    GcpAiNotebookInstanceResource createdResource =
        new WorkspaceManagerService()
            .createControlledAiNotebookInstance(
                GlobalContext.get().requireCurrentWorkspace().id, this);
    logger.info("Created AI Platform notebook: {}", createdResource);

    // convert the WSM object to a CLI object
    return new AiNotebookBuilder(createdResource).build();
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
        .deleteControlledAiNotebookInstance(
            GlobalContext.get().requireCurrentWorkspace().id, resourceId);
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

  /** Populate state information for this notebook by querying the cloud. */
  public void populateAdditionalInfo() {
    InstanceName instanceName =
        InstanceName.builder()
            .projectId(projectId)
            .location(location)
            .instanceId(instanceId)
            .build();
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(GlobalContext.get().requireCurrentTerraUser().userCredentials);
    instance = notebooks.get(instanceName);
  }

  /** Print out an AI Platform notebook resource in text format. */
  public void printText() {
    super.printText();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id:                " + projectId);
    OUT.println("AI Notebook instance location: " + location);
    OUT.println("AI Notebook instance id:       " + instanceId);
    if (instance != null) {
      OUT.println("Instance name: " + instance.getName());
      OUT.println("State:         " + instance.getState());
      OUT.println("Proxy URL:     " + instance.getProxyUri());
      OUT.println("Create time:   " + instance.getCreateTime());
    }
  }

  /** Builder class to help construct an immutable AiNotebook object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class AiNotebookBuilder extends ResourceBuilder {
    private String projectId;
    private String instanceId;
    private String location;
    private String machineType;
    private String postStartupScript;
    private Map<String, String> metadata;
    private String vmImageProject;
    private String vmImageFamily;
    private String vmImageName;
    private String containerRepository;
    private String containerTag;
    private String acceleratorType;
    private Long acceleratorCoreCount;
    private Boolean installGpuDriver;
    private String customGpuDriverPath;
    private Long bootDiskSizeGb;
    private String bootDiskType;
    private Long dataDiskSizeGb;
    private String dataDiskType;

    public AiNotebookBuilder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public AiNotebookBuilder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public AiNotebookBuilder location(String location) {
      this.location = location;
      return this;
    }

    public AiNotebookBuilder machineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public AiNotebookBuilder postStartupScript(String postStartupScript) {
      this.postStartupScript = postStartupScript;
      return this;
    }

    public AiNotebookBuilder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public AiNotebookBuilder vmImageProject(String vmImageProject) {
      this.vmImageProject = vmImageProject;
      return this;
    }

    public AiNotebookBuilder vmImageFamily(String vmImageFamily) {
      this.vmImageFamily = vmImageFamily;
      return this;
    }

    public AiNotebookBuilder vmImageName(String vmImageName) {
      this.vmImageName = vmImageName;
      return this;
    }

    public AiNotebookBuilder containerRepository(String containerRepository) {
      this.containerRepository = containerRepository;
      return this;
    }

    public AiNotebookBuilder containerTag(String containerTag) {
      this.containerTag = containerTag;
      return this;
    }

    public AiNotebookBuilder acceleratorType(String acceleratorType) {
      this.acceleratorType = acceleratorType;
      return this;
    }

    public AiNotebookBuilder acceleratorCoreCount(Long acceleratorCoreCount) {
      this.acceleratorCoreCount = acceleratorCoreCount;
      return this;
    }

    public AiNotebookBuilder installGpuDriver(Boolean installGpuDriver) {
      this.installGpuDriver = installGpuDriver;
      return this;
    }

    public AiNotebookBuilder customGpuDriverPath(String customGpuDriverPath) {
      this.customGpuDriverPath = customGpuDriverPath;
      return this;
    }

    public AiNotebookBuilder bootDiskSizeGb(Long bootDiskSizeGb) {
      this.bootDiskSizeGb = bootDiskSizeGb;
      return this;
    }

    public AiNotebookBuilder bootDiskType(String bootDiskType) {
      this.bootDiskType = bootDiskType;
      return this;
    }

    public AiNotebookBuilder dataDiskSizeGb(Long dataDiskSizeGb) {
      this.dataDiskSizeGb = dataDiskSizeGb;
      return this;
    }

    public AiNotebookBuilder dataDiskType(String dataDiskType) {
      this.dataDiskType = dataDiskType;
      return this;
    }

    /** Subclass-specific method that returns the resource type. */
    @JsonIgnore
    public ResourceType getResourceType() {
      return ResourceType.AI_NOTEBOOK;
    }

    /** Subclass-specific method that calls the sub-class constructor. */
    public AiNotebook build() {
      return new AiNotebook(this);
    }

    /** Default constructor for Jackson. */
    public AiNotebookBuilder() {
      super();
    }

    /**
     * Populate this Resource object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to AI Platform notebooks only.
     */
    public AiNotebookBuilder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      GcpAiNotebookInstanceAttributes attributes =
          wsmObject.getResourceAttributes().getGcpAiNotebookInstance();
      this.projectId = attributes.getProjectId();
      this.instanceId = attributes.getInstanceId();
      this.location = attributes.getLocation();
    }

    /**
     * Populate this Resource object with properties from the WSM GcpAiNotebookInstanceResource
     * object.
     */
    public AiNotebookBuilder(GcpAiNotebookInstanceResource wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getAttributes().getProjectId();
      this.instanceId = wsmObject.getAttributes().getInstanceId();
      this.location = wsmObject.getAttributes().getLocation();
    }
  }
}
