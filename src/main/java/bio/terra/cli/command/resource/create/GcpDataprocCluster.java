package bio.terra.cli.command.resource.create;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams.NodeConfig;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.GcpDataprocClusterCreationParameters.SoftwareFrameworkEnum;
import bio.terra.workspace.model.GcpDataprocClusterInstanceGroupConfig.PreemptibilityEnum;
import bio.terra.workspace.model.StewardshipType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import picocli.CommandLine;

/**
 * This class corresponds to the fourth-level "terra resource create gcp-dataproc-cluster" command.
 */
@CommandLine.Command(
    name = "gcp-dataproc-cluster",
    description =
        "Add a controlled GCP Dataproc cluster.\n"
            + "For a detailed explanation of parameters, see https://cloud.google.com/dataproc/docs/reference/rest/v1/projects.regions.clusters#Cluster",
    showDefaultValues = true,
    sortOptions = false)
public class GcpDataprocCluster extends WsmBaseCommand {

  @CommandLine.Mixin ResourceCreation resourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--cluster-id",
      description =
          "The unique name to give to the dataproc cluster. Cannot be changed later. "
              + "The instance name must be 1 to 52 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash. If not specified, a value will"
              + " be auto-generated for you.")
  private String clusterId;

  @CommandLine.Option(names = "--region", description = "The Google Cloud region of the cluster.")
  private String region;

  @CommandLine.Option(
      names = "--image-version",
      description =
          "The dataproc cluster image version containing versions of its software components. See https://cloud.google.com/dataproc/docs/concepts/versioning/dataproc-version-clusters for the full list of image versions and their bundled software components.")
  private String imageVersion;

  @CommandLine.Option(
      names = "--initialization-actions",
      split = ",",
      description =
          "A comma separated list of initialization scripts to run during cluster creation."
              + "The path must be a URL or Cloud Storage path, e.g. 'gs://path-to-file/file-name'.")
  private List<String> initializationActions;

  @CommandLine.Option(
      names = "--components",
      split = ",",
      description = "Comma-separated list of components.")
  private List<String> components;

  @CommandLine.Option(
      names = "--properties",
      split = ",",
      type = Map.class,
      description = "Properties in the format key=value.")
  private Map<String, String> properties;

  @CommandLine.Option(
      names = "--software-framework",
      description = "Software framework for the cluster.")
  private SoftwareFrameworkEnum softwareFramework;

  @CommandLine.Option(
      names = "--bucket",
      required = true,
      description = "Resource id of the cluster staging bucket.")
  private String configBucket;

  @CommandLine.Option(
      names = "--temp-bucket",
      required = true,
      description = "Resource id of the cluster temp bucket.")
  private String tempBucket;

  @CommandLine.Option(
      names = "--autoscaling-policy",
      description = "Autoscaling policy url to attach to the cluster.")
  private String autoscalingPolicy;

  @CommandLine.Option(
      names = {"--metadata", "-M"},
      split = ",",
      description =
          "Custom metadata to apply to this cluster.\n"
              + "specify multiple metadata in the format of --metadata=\"key1=value1\" -Mkey2=value2.\n"
              + "It allows multiple metadata entries split by \",\" like --metadata=key1=value1,key2=value2\n"
              + "By default, set Terra CLI server terra-cli-server=[CLI_SERVER_ID]\n"
              + "and the Terra workspace id (terra-workspace-id=[WORKSPACE_ID]).")
  private Map<String, String> metadata;

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Manager node configurations")
  ManagerNodeConfig managerNodeConfig = new ManagerNodeConfig();

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Worker node configurations")
  WorkerNodeConfig workerNodeConfig = new WorkerNodeConfig();

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Secondary worker node configurations")
  SecondaryWorkerNodeConfig secondaryWorkerNodeConfig = new SecondaryWorkerNodeConfig();

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Lifecycle configurations")
  LifeCycleConfig lifeCycleConfig = new LifeCycleConfig();

  /** Print this command's output in text format. */
  private static void printText(UFGcpDataprocCluster returnValue) {
    OUT.println("Successfully added controlled GCP Dataproc cluster.");
    returnValue.print();
  }

  /** Add a controlled GCP Notebook instance to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.GCP);

    System.out.println("Creating GCP Dataproc cluster...");
    System.out.println("clusterId: " + clusterId);
    System.out.println("region: " + region);
    System.out.println("imageVersion: " + imageVersion);
    System.out.println("initializationActions: " + initializationActions);
    System.out.println("components: " + components);
    System.out.println("properties: " + properties);
    System.out.println("softwareFramework: " + softwareFramework);
    System.out.println("configBucket: " + configBucket);
    System.out.println("tempBucket: " + tempBucket);
    System.out.println("autoscalingPolicy: " + autoscalingPolicy);
    System.out.println("metadata: " + metadata);

    System.out.println("managerNodeConfig.numNodes: " + managerNodeConfig.numNodes);
    System.out.println("managerNodeConfig.machineType: " + managerNodeConfig.machineType);
    System.out.println(
        "managerNodeConfig.acceleratorConfig.type: " + managerNodeConfig.acceleratorConfig.type);
    System.out.println(
        "managerNodeConfig.acceleratorConfig.count: " + managerNodeConfig.acceleratorConfig.count);
    System.out.println(
        "managerNodeConfig.diskConfig.diskSizeGb: " + managerNodeConfig.diskConfig.bootDiskSizeGb);
    System.out.println(
        "managerNodeConfig.diskConfig.diskType: " + managerNodeConfig.diskConfig.bootDiskType);
    System.out.println(
        "managerNodeConfig.diskConfig.numLocalSsds: " + managerNodeConfig.diskConfig.numLocalSsds);
    System.out.println(
        "managerNodeConfig.diskConfig.localSsdInterface "
            + managerNodeConfig.diskConfig.localSsdInterface);

    System.out.println("workerNodeConfig.numNodes: " + workerNodeConfig.numNodes);
    System.out.println("workerNodeConfig.machineType: " + workerNodeConfig.machineType);
    System.out.println(
        "workerNodeConfig.acceleratorConfig.type: " + workerNodeConfig.acceleratorConfig.type);
    System.out.println(
        "workerNodeConfig.acceleratorConfig.count: " + workerNodeConfig.acceleratorConfig.count);
    System.out.println(
        "workerNodeConfig.diskConfig.diskSizeGb: " + workerNodeConfig.diskConfig.bootDiskSizeGb);
    System.out.println(
        "workerNodeConfig.diskConfig.diskType: " + workerNodeConfig.diskConfig.bootDiskType);
    System.out.println(
        "workerNodeConfig.diskConfig.numLocalSsds: " + workerNodeConfig.diskConfig.numLocalSsds);
    System.out.println(
        "workerNodeConfig.diskConfig.localSsdInterface "
            + workerNodeConfig.diskConfig.localSsdInterface);

    System.out.println("secondaryWorkerNodeConfig.numNodes: " + secondaryWorkerNodeConfig.numNodes);
    System.out.println(
        "secondaryWorkerNodeConfig.machineType: " + secondaryWorkerNodeConfig.machineType);
    System.out.println(
        "secondaryWorkerNodeConfig.acceleratorConfig.type: "
            + secondaryWorkerNodeConfig.acceleratorConfig.type);
    System.out.println(
        "secondaryWorkerNodeConfig.acceleratorConfig.count: "
            + secondaryWorkerNodeConfig.acceleratorConfig.count);
    System.out.println(
        "secondaryWorkerNodeConfig.diskConfig.diskSizeGb: "
            + secondaryWorkerNodeConfig.diskConfig.bootDiskSizeGb);
    System.out.println(
        "secondaryWorkerNodeConfig.diskConfig.diskType: "
            + secondaryWorkerNodeConfig.diskConfig.bootDiskType);
    System.out.println(
        "secondaryWorkerNodeConfig.diskConfig.numLocalSsds: "
            + secondaryWorkerNodeConfig.diskConfig.numLocalSsds);
    System.out.println(
        "secondaryWorkerNodeConfig.diskConfig.localSsdInterface "
            + secondaryWorkerNodeConfig.diskConfig.localSsdInterface);
    System.out.println("secondaryWorkerNodeConfig.type: " + secondaryWorkerNodeConfig.type);

    System.out.println("lifeCycleConfig.idleDeleteTtl: " + lifeCycleConfig.idleDeleteTtl);
    System.out.println(
        "lifeCycleConfig.autoDeleteTtl: " + lifeCycleConfig.autoDeleteOptions.autoDeleteTtl);
    System.out.println(
        "lifeCycleConfig.autoDeleteTime: " + lifeCycleConfig.autoDeleteOptions.autoDeleteTime);

    // Retrieve the staging and temp bucket resource UUIDs and throw if they don't exist
    UUID configBucketId = Context.requireWorkspace().getResource(configBucket).getId();
    UUID tempBucketId = Context.requireWorkspace().getResource(tempBucket).getId();

    // build the resource object to create. force the resource to be private
    CreateResourceParams.Builder createResourceParams =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED)
            .accessScope(AccessScope.PRIVATE_ACCESS)
            .cloningInstructions(CloningInstructionsEnum.NOTHING);

    System.out.println(managerNodeConfig.numNodes);
    System.out.println(managerNodeConfig);

    // build the dataproc creation parameters
    CreateGcpDataprocClusterParams.Builder createParams =
        new CreateGcpDataprocClusterParams.Builder()
            .resourceFields(createResourceParams.build())
            .clusterId(clusterId)
            .region(region)
            .imageVersion(imageVersion)
            .initializationActions(initializationActions)
            .components(components)
            .properties(properties)
            .softwareFramework(softwareFramework)
            .configBucket(configBucketId)
            .tempBucket(tempBucketId)
            .autoscalingPolicy(autoscalingPolicy)
            .metadata(metadata)
            .managerConfig(
                new NodeConfig.Builder()
                    .numNodes(managerNodeConfig.numNodes)
                    .machineType(managerNodeConfig.machineType)
                    .imageUri(managerNodeConfig.imageUri)
                    .acceleratorConfig(
                        Optional.ofNullable(managerNodeConfig.acceleratorConfig)
                            .filter(ac -> ac.type != null && ac.count > 0)
                            .map(
                                ac ->
                                    new CreateGcpDataprocClusterParams.AcceleratorConfig.Builder()
                                        .type(ac.type)
                                        .count(ac.count)
                                        .build())
                            .orElse(null))
                    .diskConfig(
                        new CreateGcpDataprocClusterParams.DiskConfig.Builder()
                            .bootDiskType(managerNodeConfig.diskConfig.bootDiskType)
                            .bootDiskSizeGb(managerNodeConfig.diskConfig.bootDiskSizeGb)
                            .numLocalSsds(managerNodeConfig.diskConfig.numLocalSsds)
                            .localSsdInterface(managerNodeConfig.diskConfig.localSsdInterface)
                            .build())
                    .build())
            .workerConfig(
                new NodeConfig.Builder()
                    .numNodes(workerNodeConfig.numNodes)
                    .machineType(workerNodeConfig.machineType)
                    .imageUri(workerNodeConfig.imageUri)
                    .acceleratorConfig(
                        Optional.ofNullable(workerNodeConfig.acceleratorConfig)
                            .filter(ac -> ac.type != null && ac.count > 0)
                            .map(
                                ac ->
                                    new CreateGcpDataprocClusterParams.AcceleratorConfig.Builder()
                                        .type(ac.type)
                                        .count(ac.count)
                                        .build())
                            .orElse(null))
                    .diskConfig(
                        new CreateGcpDataprocClusterParams.DiskConfig.Builder()
                            .bootDiskType(workerNodeConfig.diskConfig.bootDiskType)
                            .bootDiskSizeGb(workerNodeConfig.diskConfig.bootDiskSizeGb)
                            .numLocalSsds(workerNodeConfig.diskConfig.numLocalSsds)
                            .localSsdInterface(workerNodeConfig.diskConfig.localSsdInterface)
                            .build())
                    .build())
            .secondaryWorkerConfig(
                new NodeConfig.Builder()
                    .numNodes(secondaryWorkerNodeConfig.numNodes)
                    .machineType(secondaryWorkerNodeConfig.machineType)
                    .imageUri(secondaryWorkerNodeConfig.imageUri)
                    .preemptibility(secondaryWorkerNodeConfig.type)
                    .acceleratorConfig(
                        Optional.ofNullable(secondaryWorkerNodeConfig.acceleratorConfig)
                            .filter(ac -> ac.type != null && ac.count > 0)
                            .map(
                                ac ->
                                    new CreateGcpDataprocClusterParams.AcceleratorConfig.Builder()
                                        .type(ac.type)
                                        .count(ac.count)
                                        .build())
                            .orElse(null))
                    .diskConfig(
                        new CreateGcpDataprocClusterParams.DiskConfig.Builder()
                            .bootDiskType(secondaryWorkerNodeConfig.diskConfig.bootDiskType)
                            .bootDiskSizeGb(secondaryWorkerNodeConfig.diskConfig.bootDiskSizeGb)
                            .numLocalSsds(secondaryWorkerNodeConfig.diskConfig.numLocalSsds)
                            .localSsdInterface(
                                secondaryWorkerNodeConfig.diskConfig.localSsdInterface)
                            .build())
                    .build())
            .lifeCycleConfig(
                new CreateGcpDataprocClusterParams.LifeCycleConfig.Builder()
                    .idleDeleteTtl(lifeCycleConfig.idleDeleteTtl)
                    .autoDeleteTtl(lifeCycleConfig.autoDeleteOptions.autoDeleteTtl)
                    .autoDeleteTime(lifeCycleConfig.autoDeleteOptions.autoDeleteTime)
                    .build());

    bio.terra.cli.businessobject.resource.GcpDataprocCluster createdResource =
        bio.terra.cli.businessobject.resource.GcpDataprocCluster.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFGcpDataprocCluster(createdResource), GcpDataprocCluster::printText);
  }

  static class ManagerNodeConfig {
    // Number of manager nodes cannot be specified by the user. It is always 1.
    private final int numNodes = 1;

    @CommandLine.Option(
        names = "--manager-machine-type",
        description = "The machine type of the manager node.",
        defaultValue = "n2-standard-2")
    private String machineType;

    @CommandLine.Option(
        names = "--manager-image-uri",
        description = "The image URI for the manager node.")
    private String imageUri;

    @CommandLine.ArgGroup(exclusive = false)
    private ManagerAcceleratorConfig acceleratorConfig = new ManagerAcceleratorConfig();

    @CommandLine.ArgGroup(exclusive = false)
    private ManagerDiskConfig diskConfig = new ManagerDiskConfig();

    static class ManagerAcceleratorConfig {

      @CommandLine.Option(
          names = "--manager-accelerator-type",
          description = "The type of accelerator for the manager.")
      private String type;

      @CommandLine.Option(
          names = "--manager-accelerator-count",
          description = "The count of accelerators for the manager.")
      private int count;
    }

    static class ManagerDiskConfig {

      @CommandLine.Option(
          names = "--manager-boot-disk-type",
          description = "The type of boot disk for the manager node.")
      private String bootDiskType;

      @CommandLine.Option(
          names = "--manager-boot-disk-size",
          defaultValue = "500",
          description = "The size of the boot disk in GB for the manager node.")
      private int bootDiskSizeGb;

      @CommandLine.Option(
          names = "--manager-num-local-ssds",
          description = "The number of local SSDs for the manager node.")
      private int numLocalSsds;

      @CommandLine.Option(
          names = "--manager-local-ssd-interface",
          description = "The interface type of local SSDs for the manager node.",
          defaultValue = "scsi")
      private String localSsdInterface;
    }
  }

  static class WorkerNodeConfig {
    @CommandLine.Option(
        names = "--num-workers",
        description = "The number of worker nodes.",
        defaultValue = "2")
    private int numNodes;

    @CommandLine.Option(
        names = "--worker-machine-type",
        description = "The machine type of the worker node.",
        defaultValue = "n2-standard-2")
    private String machineType;

    @CommandLine.Option(
        names = "--worker-image-uri",
        description = "The image URI for the worker node.")
    private String imageUri;

    @CommandLine.ArgGroup(exclusive = false)
    private WorkerAcceleratorConfig acceleratorConfig = new WorkerAcceleratorConfig();

    @CommandLine.ArgGroup(exclusive = false)
    private WorkerDiskConfig diskConfig = new WorkerDiskConfig();

    static class WorkerAcceleratorConfig {
      @CommandLine.Option(
          names = "--worker-accelerator-type",
          description = "The type of accelerator for the worker.")
      private String type;

      @CommandLine.Option(
          names = "--worker-accelerator-count",
          description = "The count of accelerators for the worker.")
      private int count;
    }

    static class WorkerDiskConfig {
      @CommandLine.Option(
          names = "--worker-boot-disk-type",
          description = "The type of boot disk for the worker node.")
      private String bootDiskType;

      @CommandLine.Option(
          names = "--worker-boot-disk-size",
          description = "The size of the boot disk in GB for the worker node.",
          defaultValue = "500")
      private int bootDiskSizeGb;

      @CommandLine.Option(
          names = "--worker-num-local-ssds",
          description = "The number of local SSDs for the worker node.")
      private int numLocalSsds;

      @CommandLine.Option(
          names = "--worker-local-ssd-interface",
          description = "The interface type of local SSDs for the worker node.",
          defaultValue = "scsi")
      private String localSsdInterface;
    }
  }

  static class SecondaryWorkerNodeConfig {
    @CommandLine.Option(
        names = "--num-secondary-workers",
        description = "The number of secondary worker nodes.")
    private int numNodes;

    @CommandLine.Option(
        names = "--secondary-worker-machine-type",
        description = "The machine type of the secondary worker node.",
        defaultValue = "n2-standard-2")
    private String machineType;

    @CommandLine.Option(
        names = "--secondary-worker-image-uri",
        description = "The image URI for the secondary worker node.")
    private String imageUri;

    @CommandLine.Option(
        names = "--secondary-worker-type",
        description =
            "The type of the secondary worker. Valid values are preemptible, non-preemptible, and spot.",
        defaultValue = "spot",
        converter = PreemptibilityEnumConverter.class)
    private PreemptibilityEnum type;

    @CommandLine.ArgGroup(exclusive = false)
    private SecondaryWorkerAcceleratorConfig acceleratorConfig =
        new SecondaryWorkerAcceleratorConfig();

    @CommandLine.ArgGroup(exclusive = false)
    private SecondaryWorkerDiskConfig diskConfig = new SecondaryWorkerDiskConfig();

    static class SecondaryWorkerAcceleratorConfig {
      @CommandLine.Option(
          names = "--secondary-worker-accelerator-type",
          description = "The type of accelerator for the secondary worker.")
      private String type;

      @CommandLine.Option(
          names = "--secondary-worker-accelerator-count",
          description = "The count of accelerators for the secondary worker.")
      private int count;
    }

    static class SecondaryWorkerDiskConfig {
      @CommandLine.Option(
          names = "--secondary-worker-boot-disk-type",
          description = "The type of boot disk for the secondary worker node.")
      private String bootDiskType;

      @CommandLine.Option(
          names = "--secondary-worker-boot-disk-size",
          description = "The size of the boot disk in GB for the secondary worker node.",
          defaultValue = "500")
      private int bootDiskSizeGb;

      @CommandLine.Option(
          names = "--secondary-worker-num-local-ssds",
          description = "The number of local SSDs for the secondary worker node.")
      private int numLocalSsds;

      @CommandLine.Option(
          names = "--secondary-worker-local-ssd-interface",
          description = "The interface type of local SSDs for the secondary worker node.",
          defaultValue = "scsi")
      private String localSsdInterface;
    }
  }

  /** Helper class to convert an any cased string to a {@link PreemptibilityEnum}. */
  static class PreemptibilityEnumConverter
      implements CommandLine.ITypeConverter<PreemptibilityEnum> {
    @Override
    public PreemptibilityEnum convert(String value) throws Exception {
      return PreemptibilityEnum.valueOf(value.replace("-", "_").toUpperCase());
    }
  }

  static class LifeCycleConfig {
    @CommandLine.Option(
        names = "--idle-delete-ttl",
        description = "Time-to-live after which the resource becomes idle and is deleted.")
    private String idleDeleteTtl;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
    private AutoDeleteOptions autoDeleteOptions = new AutoDeleteOptions();

    static class AutoDeleteOptions {
      @CommandLine.Option(
          names = "--auto-delete-ttl",
          description = "Time-to-live after which the resource is automatically deleted.")
      private String autoDeleteTtl;

      @CommandLine.Option(
          names = "--auto-delete-time",
          description = "Specific date and time after which the resource is automatically deleted.",
          converter = OffsetDateTimeConverter.class)
      private OffsetDateTime autoDeleteTime;
    }
  }

  /** Helper class to convert a string to an {@link OffsetDateTime}. */
  static class OffsetDateTimeConverter implements CommandLine.ITypeConverter<OffsetDateTime> {
    @Override
    public OffsetDateTime convert(String value) throws UserActionableException {
      return OffsetDateTime.parse(value);
    }
  }
}
