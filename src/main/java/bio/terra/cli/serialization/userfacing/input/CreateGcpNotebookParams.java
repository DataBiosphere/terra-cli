package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * Parameters for creating a GCP notebook workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateGcpNotebookParams.Builder.class)
public class CreateGcpNotebookParams {
  public final CreateResourceParams resourceFields;
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

  protected CreateGcpNotebookParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
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

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
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

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
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

    public Builder machineType(String machineType) {
      this.machineType = machineType;
      return this;
    }

    public Builder postStartupScript(String postStartupScript) {
      this.postStartupScript = postStartupScript;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder vmImageProject(String vmImageProject) {
      this.vmImageProject = vmImageProject;
      return this;
    }

    public Builder vmImageFamily(String vmImageFamily) {
      this.vmImageFamily = vmImageFamily;
      return this;
    }

    public Builder vmImageName(String vmImageName) {
      this.vmImageName = vmImageName;
      return this;
    }

    public Builder containerRepository(String containerRepository) {
      this.containerRepository = containerRepository;
      return this;
    }

    public Builder containerTag(String containerTag) {
      this.containerTag = containerTag;
      return this;
    }

    public Builder acceleratorType(String acceleratorType) {
      this.acceleratorType = acceleratorType;
      return this;
    }

    public Builder acceleratorCoreCount(Long acceleratorCoreCount) {
      this.acceleratorCoreCount = acceleratorCoreCount;
      return this;
    }

    public Builder installGpuDriver(Boolean installGpuDriver) {
      this.installGpuDriver = installGpuDriver;
      return this;
    }

    public Builder customGpuDriverPath(String customGpuDriverPath) {
      this.customGpuDriverPath = customGpuDriverPath;
      return this;
    }

    public Builder bootDiskSizeGb(Long bootDiskSizeGb) {
      this.bootDiskSizeGb = bootDiskSizeGb;
      return this;
    }

    public Builder bootDiskType(String bootDiskType) {
      this.bootDiskType = bootDiskType;
      return this;
    }

    public Builder dataDiskSizeGb(Long dataDiskSizeGb) {
      this.dataDiskSizeGb = dataDiskSizeGb;
      return this;
    }

    public Builder dataDiskType(String dataDiskType) {
      this.dataDiskType = dataDiskType;
      return this;
    }

    /** Call the private constructor. */
    public CreateGcpNotebookParams build() {
      return new CreateGcpNotebookParams(this);
    }
  }
}
