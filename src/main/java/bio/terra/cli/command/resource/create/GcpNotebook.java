package bio.terra.cli.command.resource.create;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.StewardshipType;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create gcp-notebook" command. */
@CommandLine.Command(
    name = "gcp-notebook",
    description =
        "Add a controlled GCP notebook instance resource.\n"
            + "For a detailed explanation of some parameters, see https://cloud.google.com/vertex-ai/docs/workbench/reference/rest/v1/projects.locations.instances#Instance.",
    showDefaultValues = true,
    sortOptions = false)
public class GcpNotebook extends BaseCommand {

  private static final String DEFAULT_VM_IMAGE_PROJECT = "deeplearning-platform-release";
  private static final String DEFAULT_VM_IMAGE_FAMILY = "r-latest-cpu-experimental";

  // Use CreateResource instead of createControlledResource because only private notebooks are
  // supported and we don't want to provide options that are not useful.
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;

  @CommandLine.Option(
      names = "--instance-id",
      description =
          "The unique name to give to the notebook instance. Cannot be changed later. "
              + "The instance name must be 1 to 63 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash. If not specified, an "
              + "auto-generated name based on your email address and time will be used.")
  private String instanceId;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description =
          "The Google Cloud location of the instance (https://cloud.google.com/vertex-ai/docs/general/locations#user-managed-notebooks-locations).")
  private String location;

  @CommandLine.Option(
      names = "--machine-type",
      defaultValue = "n1-standard-4",
      description = "The Compute Engine machine type of this instance.")
  private String machineType;

  @CommandLine.Option(
      names = "--post-startup-script",
      defaultValue =
          "https://raw.githubusercontent.com/DataBiosphere/terra-cli/main/notebooks/post-startup.sh",
      description =
          "Path to a Bash script that automatically runs after a notebook instance fully boots up. "
              + "The path must be a URL or Cloud Storage path, e.g. 'gs://path-to-file/file-name'.")
  private String postStartupScript;

  @CommandLine.Option(
      names = "--metadata",
      description =
          "Custom metadata to apply to this instance.\nBy default sets some jupyterlab extensions "
              + "(installed-extensions=jupyterlab_bigquery-latest.tar.gz,jupyterlab_gcsfilebrowser-latest.tar.gz,jupyterlab_gcpscheduler-latest.tar.gz) "
              + "and the Terra workspace id (terra-workspace-id=[WORKSPACE_ID]).")
  private Map<String, String> metadata;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  GcpNotebook.VmOrContainerImage vmOrContainerImage;

  static class VmOrContainerImage {
    @CommandLine.ArgGroup(
        exclusive = false,
        multiplicity = "1",
        heading =
            "Definition of a custom Compute Engine virtual machine image for starting a "
                + "notebook instance with the environment installed directly on the VM.\n"
                + "If neither this nor --container-* are specified, default to \n"
                + "'--vm-image-project="
                + DEFAULT_VM_IMAGE_PROJECT
                + " --vm-image-family="
                + DEFAULT_VM_IMAGE_FAMILY
                + "'.%n")
    VmImage vm;

    @CommandLine.ArgGroup(
        exclusive = false,
        multiplicity = "1",
        heading =
            "Definition of a container image for starting a notebook instance with the environment "
                + "installed in a container.%n")
    ContainerImage container;
  }

  static class VmImage {
    @CommandLine.Option(
        names = "--vm-image-project",
        required = true,
        description = "The ID of the Google Cloud project that this VM image belongs to.")
    private String project;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    ImageConfig imageConfig;

    static class ImageConfig {
      @CommandLine.Option(
          names = "--vm-image-family",
          description =
              "Use this VM image family to find the image; the newest image in this family will be "
                  + "used.")
      private String family;

      @CommandLine.Option(
          names = "--vm-image-name",
          description = "Use this VM image name to find the image.")
      private String name;
    }
  }

  static class ContainerImage {
    @CommandLine.Option(
        names = "--container-repository",
        required = true,
        description =
            "The path to the container image repository. For example: "
                + "'gcr.io/{project_id}/{imageName}'.")
    private String repository;

    @CommandLine.Option(
        names = "--container-tag",
        description =
            "The tag of the container image. If not specified, this defaults to the latest tag.")
    private String tag;
  }

  // TODO(PF-767): Consider how to improve usability & validation of these parameters.
  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "The hardware accelerator used on this instance.%n")
  GcpNotebook.AcceleratorConfig acceleratorConfig;

  static class AcceleratorConfig {
    @CommandLine.Option(names = "--accelerator-type", description = "Type of this accelerator.")
    private String type;

    @CommandLine.Option(
        names = "--accelerator-core-count",
        description = "Count of cores of this accelerator.")
    private Long coreCount;
  }

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "GPU driver configurations.%n")
  GcpNotebook.GpuDriverConfiguration gpuDriverConfiguration;

  static class GpuDriverConfiguration {
    @CommandLine.Option(
        names = "--install-gpu-driver",
        description =
            "If true, the end user authorizes Google Cloud to install a GPU driver on this instance.")
    private Boolean installGpuDriver;

    @CommandLine.Option(
        names = "--custom-gpu-driver-path",
        description = "Specify a custom Cloud Storage path where the GPU driver is stored.")
    private String customGpuDriverPath;
  }

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Boot disk configurations.%n")
  GcpNotebook.BootDiskConfiguration bootDiskConfiguration;

  static class BootDiskConfiguration {
    @CommandLine.Option(
        names = "--boot-disk-size",
        description = "The size of the disk in GB attached to this instance.")
    Long sizeGb;

    @CommandLine.Option(
        names = "--boot-disk-type",
        description =
            "The type of disk attached to this instance, defaults to the standard persistent disk.")
    String type;
  }

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Data disk configurations.%n")
  GcpNotebook.DataDiskConfiguration dataDiskConfiguration;

  static class DataDiskConfiguration {
    @CommandLine.Option(
        names = "--data-disk-size",
        description = "The size of the disk in GB attached to this instance.")
    Long sizeGb;

    @CommandLine.Option(
        names = "--data-disk-type",
        description =
            "The type of disk attached to this instance, defaults to the standard persistent disk.")
    String type;
  }

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a controlled GCP Notebook instance to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to create. force the resource to be private
    CreateResourceParams.Builder createResourceParams =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED)
            .accessScope(AccessScope.PRIVATE_ACCESS);
    CreateGcpNotebookParams.Builder createParams =
        new CreateGcpNotebookParams.Builder()
            .resourceFields(createResourceParams.build())
            .instanceId(instanceId)
            .location(location)
            .machineType(machineType)
            .postStartupScript(postStartupScript)
            .metadata(
                metadata == null ? defaultMetadata(Context.requireWorkspace().getId()) : metadata);

    if (acceleratorConfig != null) {
      createParams
          .acceleratorType(acceleratorConfig.type)
          .acceleratorCoreCount(acceleratorConfig.coreCount);
    }
    if (gpuDriverConfiguration != null) {
      createParams
          .installGpuDriver(gpuDriverConfiguration.installGpuDriver)
          .customGpuDriverPath(gpuDriverConfiguration.customGpuDriverPath);
    }
    if (bootDiskConfiguration != null) {
      createParams
          .bootDiskType(bootDiskConfiguration.type)
          .bootDiskSizeGb(bootDiskConfiguration.sizeGb);
    }
    if (dataDiskConfiguration != null) {
      createParams
          .dataDiskType(dataDiskConfiguration.type)
          .dataDiskSizeGb(dataDiskConfiguration.sizeGb);
    }
    if (vmOrContainerImage == null) {
      createParams.vmImageProject(DEFAULT_VM_IMAGE_PROJECT).vmImageFamily(DEFAULT_VM_IMAGE_FAMILY);
    } else if (vmOrContainerImage.container != null) {
      createParams
          .containerRepository(vmOrContainerImage.container.repository)
          .containerTag(vmOrContainerImage.container.tag);
    } else {
      createParams
          .vmImageProject(vmOrContainerImage.vm.project)
          .vmImageFamily(vmOrContainerImage.vm.imageConfig.family)
          .vmImageName(vmOrContainerImage.vm.imageConfig.name);
    }

    bio.terra.cli.businessobject.resource.GcpNotebook createdResource =
        bio.terra.cli.businessobject.resource.GcpNotebook.createControlled(createParams.build());
    formatOption.printReturnValue(new UFGcpNotebook(createdResource), GcpNotebook::printText);
  }

  /** Create the metadata to put on the GCP Notebook instance. */
  private Map<String, String> defaultMetadata(UUID workspaceID) {
    return ImmutableMap.<String, String>builder()
        // Set additional Terra context as metadata on the VM instance.
        .put("terra-workspace-id", workspaceID.toString())
        .put("terra-cli-server", Context.getServer().getName())
        .build();
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcpNotebook returnValue) {
    OUT.println("Successfully added controlled GCP Notebook instance.");
    returnValue.print();
  }
}
