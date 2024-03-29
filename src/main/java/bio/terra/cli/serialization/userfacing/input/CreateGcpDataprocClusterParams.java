package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.GcpDataprocClusterAcceleratorConfig;
import bio.terra.workspace.model.GcpDataprocClusterCreationParameters;
import bio.terra.workspace.model.GcpDataprocClusterCreationParameters.SoftwareFrameworkEnum;
import bio.terra.workspace.model.GcpDataprocClusterDiskConfig;
import bio.terra.workspace.model.GcpDataprocClusterInstanceGroupConfig;
import bio.terra.workspace.model.GcpDataprocClusterInstanceGroupConfig.PreemptibilityEnum;
import bio.terra.workspace.model.GcpDataprocClusterLifecycleConfig;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Parameters for creating a GCP dataproc cluster workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
@JsonDeserialize(builder = CreateGcpDataprocClusterParams.Builder.class)
public class CreateGcpDataprocClusterParams {

  public final CreateResourceParams resourceFields;
  public final String clusterId;
  public final String region;
  public final String imageVersion;
  public final List<String> initializationActions;
  public final List<String> components;
  public final Map<String, String> properties;
  public final SoftwareFrameworkEnum softwareFramework;
  public final UUID configBucket;
  public final UUID tempBucket;
  public final String autoscalingPolicy;
  public final Map<String, String> metadata;

  // Node configurations
  public final NodeConfig managerConfig;
  public final NodeConfig workerConfig;
  public final NodeConfig secondaryWorkerConfig;

  public final String idleDeleteTtl;

  protected CreateGcpDataprocClusterParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.clusterId = builder.clusterId;
    this.region = builder.region;
    this.imageVersion = builder.imageVersion;
    this.initializationActions = builder.initializationActions;
    this.components = builder.components;
    this.properties = builder.properties;
    this.softwareFramework = builder.softwareFramework;
    this.configBucket = builder.configBucket;
    this.tempBucket = builder.tempBucket;
    this.autoscalingPolicy = builder.autoscalingPolicy;
    this.metadata = builder.metadata;
    this.managerConfig = builder.managerConfig;
    this.workerConfig = builder.workerConfig;
    this.secondaryWorkerConfig = builder.secondaryWorkerConfig;
    this.idleDeleteTtl = builder.idleDeleteTtl;
  }

  public record AcceleratorConfig(String type, int count) {
    public static class Builder {
      private String type;
      private int count;

      public Builder type(String type) {
        this.type = type;
        return this;
      }

      public Builder count(int count) {
        this.count = count;
        return this;
      }

      public AcceleratorConfig build() {
        return new AcceleratorConfig(type, count);
      }
    }
  }

  public record DiskConfig(
      String bootDiskType, int bootDiskSizeGb, int numLocalSsds, String localSsdInterface) {
    public static class Builder {
      private String bootDiskType;
      private int bootDiskSizeGb;
      private int numLocalSsds;
      private String localSsdInterface;

      public Builder bootDiskType(String bootDiskType) {
        this.bootDiskType = bootDiskType;
        return this;
      }

      public Builder bootDiskSizeGb(int bootDiskSizeGb) {
        this.bootDiskSizeGb = bootDiskSizeGb;
        return this;
      }

      public Builder numLocalSsds(int numLocalSsds) {
        this.numLocalSsds = numLocalSsds;
        return this;
      }

      public Builder localSsdInterface(String localSsdInterface) {
        this.localSsdInterface = localSsdInterface;
        return this;
      }

      public DiskConfig build() {
        return new DiskConfig(bootDiskType, bootDiskSizeGb, numLocalSsds, localSsdInterface);
      }
    }
  }

  public record NodeConfig(
      int numNodes,
      String machineType,
      String imageUri,
      AcceleratorConfig acceleratorConfig,
      DiskConfig diskConfig,
      @Nullable PreemptibilityEnum preemptibility) {
    public static class Builder {
      private int numNodes;
      private String machineType;
      private String imageUri;
      private AcceleratorConfig acceleratorConfig;
      private DiskConfig diskConfig;
      private PreemptibilityEnum preemptibility;

      public Builder numNodes(int numNodes) {
        this.numNodes = numNodes;
        return this;
      }

      public Builder machineType(String machineType) {
        this.machineType = machineType;
        return this;
      }

      public Builder imageUri(String imageUri) {
        this.imageUri = imageUri;
        return this;
      }

      public Builder acceleratorConfig(AcceleratorConfig acceleratorConfig) {
        this.acceleratorConfig = acceleratorConfig;
        return this;
      }

      public Builder diskConfig(DiskConfig diskConfig) {
        this.diskConfig = diskConfig;
        return this;
      }

      public Builder preemptibility(PreemptibilityEnum preemptibility) {
        this.preemptibility = preemptibility;
        return this;
      }

      public NodeConfig build() {
        return new NodeConfig(
            numNodes, machineType, imageUri, acceleratorConfig, diskConfig, preemptibility);
      }
    }
  }

  public static class Builder {
    private CreateResourceParams resourceFields;
    private String clusterId;
    private String region;
    private String imageVersion;
    private List<String> initializationActions;
    private List<String> components;
    private Map<String, String> properties;
    private SoftwareFrameworkEnum softwareFramework;
    private UUID configBucket;
    private UUID tempBucket;
    private String autoscalingPolicy;
    private Map<String, String> metadata;
    private NodeConfig managerConfig;
    private NodeConfig workerConfig;
    private NodeConfig secondaryWorkerConfig;

    private String idleDeleteTtl;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder clusterId(String clusterId) {
      this.clusterId = clusterId;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder imageVersion(String imageVersion) {
      this.imageVersion = imageVersion;
      return this;
    }

    public Builder initializationActions(List<String> initializationActions) {
      this.initializationActions = initializationActions;
      return this;
    }

    public Builder components(List<String> components) {
      this.components = components;
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public Builder softwareFramework(SoftwareFrameworkEnum softwareFramework) {
      this.softwareFramework = softwareFramework;
      return this;
    }

    public Builder configBucket(UUID configBucket) {
      this.configBucket = configBucket;
      return this;
    }

    public Builder tempBucket(UUID tempBucket) {
      this.tempBucket = tempBucket;
      return this;
    }

    public Builder autoscalingPolicy(String autoscalingPolicy) {
      this.autoscalingPolicy = autoscalingPolicy;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder managerConfig(NodeConfig managerConfig) {
      this.managerConfig = managerConfig;
      return this;
    }

    public Builder workerConfig(NodeConfig workerConfig) {
      this.workerConfig = workerConfig;
      return this;
    }

    public Builder secondaryWorkerConfig(NodeConfig secondaryWorkerConfig) {
      this.secondaryWorkerConfig = secondaryWorkerConfig;
      return this;
    }

    public Builder idleDeleteTtl(String idleDeleteTtl) {
      this.idleDeleteTtl = idleDeleteTtl;
      return this;
    }

    public CreateGcpDataprocClusterParams build() {
      return new CreateGcpDataprocClusterParams(this);
    }
  }

  /**
   * This method converts this CLI-defined POJO class into the WSM client library-defined request
   * object.
   *
   * @return GCP Dataproc cluster creation parameters in the format expected by the WSM client
   *     library
   */
  public static GcpDataprocClusterCreationParameters toWsmCreateClusterParams(
      CreateGcpDataprocClusterParams createParams) {
    return new GcpDataprocClusterCreationParameters()
        .clusterId(createParams.clusterId)
        .region(createParams.region)
        .imageVersion(createParams.imageVersion)
        .initializationScripts(createParams.initializationActions)
        .components(createParams.components)
        .properties(createParams.properties)
        .softwareFramework(createParams.softwareFramework)
        .configBucket(createParams.configBucket)
        .tempBucket(createParams.tempBucket)
        .autoscalingPolicy(createParams.autoscalingPolicy)
        .metadata(createParams.metadata)
        .managerNodeConfig(buildInstanceGroupConfig(createParams.managerConfig))
        .primaryWorkerConfig(buildInstanceGroupConfig(createParams.workerConfig))
        .secondaryWorkerConfig(buildInstanceGroupConfig(createParams.secondaryWorkerConfig))
        .lifecycleConfig(
            new GcpDataprocClusterLifecycleConfig().idleDeleteTtl(createParams.idleDeleteTtl));
  }

  private static GcpDataprocClusterInstanceGroupConfig buildInstanceGroupConfig(
      CreateGcpDataprocClusterParams.NodeConfig nodeConfig) {
    return new GcpDataprocClusterInstanceGroupConfig()
        .numInstances(nodeConfig.numNodes())
        .machineType(nodeConfig.machineType())
        .imageUri(nodeConfig.imageUri())
        .acceleratorConfig(
            Optional.ofNullable(nodeConfig.acceleratorConfig())
                .map(
                    ac ->
                        new GcpDataprocClusterAcceleratorConfig()
                            .type(ac.type())
                            .cardCount(ac.count()))
                .orElse(null))
        .diskConfig(
            new GcpDataprocClusterDiskConfig()
                .bootDiskType(nodeConfig.diskConfig().bootDiskType())
                .bootDiskType(nodeConfig.diskConfig().bootDiskType())
                .bootDiskSizeGb(nodeConfig.diskConfig().bootDiskSizeGb())
                .numLocalSsds(nodeConfig.diskConfig().numLocalSsds())
                .localSsdInterface(nodeConfig.diskConfig().localSsdInterface()))
        .preemptibility(nodeConfig.preemptibility());
  }
}
