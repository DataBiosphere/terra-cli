package bio.terra.cli.command.resource.create;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.StewardshipType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create gcp-notebook" command. */
@CommandLine.Command(
    name = "gcp-notebook",
    description =
        "Add a controlled GCP notebook instance.\n"
            + "For a detailed explanation of some parameters, see https://cloud.google.com/vertex-ai/docs/workbench/reference/rest/v1/projects.locations.instances#Instance.",
    showDefaultValues = true,
    sortOptions = false)
public class GcpNotebook extends WsmBaseCommand {
  private static final String DEFAULT_VM_IMAGE_PROJECT = "deeplearning-platform-release";
  private static final String DEFAULT_VM_IMAGE_FAMILY = "r-latest-cpu-experimental";

  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  GcpNotebook.VmOrContainerImage vmOrContainerImage;
  // TODO(PF-767): Consider how to improve usability & validation of these parameters.
  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "The hardware accelerator used on this instance.%n")
  GcpNotebook.AcceleratorConfig acceleratorConfig;

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "GPU driver configurations.%n")
  GcpNotebook.GpuDriverConfiguration gpuDriverConfiguration;

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Boot disk configurations.%n")
  GcpNotebook.BootDiskConfiguration bootDiskConfiguration;

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Data disk configurations.%n")
  GcpNotebook.DataDiskConfiguration dataDiskConfiguration;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

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
      description =
          "The Compute Engine machine type of this instance (https://cloud.google.com/compute/docs/general-purpose-machines).")
  private String machineType;

  @CommandLine.Option(
      names = "--post-startup-script",
      description =
          "Path to a Bash script that automatically runs after a notebook instance fully boots up. "
              + "The path must be a URL or Cloud Storage path, e.g. 'gs://path-to-file/file-name'.",
      defaultValue =
          "https://raw.githubusercontent.com/DataBiosphere/terra-workspace-manager/main/service/src/main/java/bio/terra/workspace/service/resource/controlled/cloud/gcp/ainotebook/post-startup.sh")
  private String postStartupScript;

  @CommandLine.Option(
      names = {"--metadata", "-M"},
      split = ",",
      description =
          "Custom metadata to apply to this instance.\n"
              + "specify multiple metadata in the format of --metadata=\"key1=value1\" -Mkey2=value2.\n"
              + "It allows multiple metadata entries split by \",\" like --metadata=key1=value1,key2=value2\n"
              + "By default set Terra CLI server terra-cli-server=[CLI_SERVER_ID]\n"
              + "and the Terra workspace id (terra-workspace-id=[WORKSPACE_ID]).")
  private Map<String, String> metadata;

  /** Print this command's output in text format. */
  private static void printText(UFGcpNotebook returnValue) {
    OUT.println("Successfully added controlled GCP Notebook instance.");
    returnValue.print();
  }

  /** Add a controlled GCP Notebook instance to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.GCP);

    // build the resource object to create. force the resource to be private
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
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
            .metadata(Optional.ofNullable(metadata).orElse(Collections.emptyMap()));

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

    boolean doWaitForNotebooksReady = true; // this would be a flag
    if (doWaitForNotebooksReady) {
      waitForNotebooksReady(createdResource.getInstanceId());
    }
  }

  private String getNotebookGuestAttribute(String instanceId, String guestAttribute) {
    String command =
        "gcloud compute instances get-guest-attributes "
            + instanceId
            + " "
            + "--zone us-central1-a "
            + "--query-path '"
            + guestAttribute
            + "' "
            + "--format 'json(value)'";

    List<String> processCommand = new ArrayList<>();
    processCommand.add("bash");
    processCommand.add("-ce");
    processCommand.add(command);

    Map<String, String> envVars = new HashMap<String, String>();

    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    localProcessLauncher.launchProcess(processCommand, envVars);

    InputStream stdout = localProcessLauncher.getInputStream();
    int exitCode = localProcessLauncher.waitForTerminate();
    if (exitCode != 0) {
      // This is actually expected for several iterations immediately
      // after provisioning.
      ERR.println(String.format("ERROR getting notebook startup status: %d", exitCode));
      return "";
    }

    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(localProcessLauncher.getInputStream()));

    String output;
    try {
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
      reader.close();

      output = sb.toString().trim();
    } catch (IOException ex) {
      // TODO:
      return null;
    }

    if (output.isEmpty()) {
      return "";
    } else {
      // Remove the starting and trailing square brackets
      // and then parse as JSON.
      String jsonstr = output.substring(1, output.length() - 1);
      JSONObject obj = new JSONObject(jsonstr);
      return obj.getString("value").trim();
    }
  }

  private String getNotebookReadyStatus(String instanceId) {
    return getNotebookGuestAttribute(instanceId, "startup_script/status");
  }

  private String getNotebookReadyMessage(String instanceId) {
    return getNotebookGuestAttribute(instanceId, "startup_script/message");
  }

  private void waitForNotebooksReady(String instanceId) {
    String lastStatus = "";
    while (true) {
      String currStatus = getNotebookReadyStatus(instanceId);

      if (currStatus.isEmpty()) {
        OUT.println("The post-startup script has not started.");
      } else if (currStatus.equals(lastStatus)) {
        OUT.print(".");
      } else {
        OUT.println();
        OUT.print(currStatus);

        if (currStatus.equals("COMPLETE")) {
          OUT.println();
          break;
        } else if (currStatus.equals("ERROR")) {
          OUT.println();
          OUT.println(getNotebookReadyMessage(instanceId));
          break;
        }

        lastStatus = currStatus;
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException ex) {
        ERR.println(ex);
        break;
      }
    }
  }

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
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    ImageConfig imageConfig;

    @CommandLine.Option(
        names = "--vm-image-project",
        required = true,
        description = "The ID of the Google Cloud project that this VM image belongs to.")
    private String project;

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

  static class AcceleratorConfig {
    @CommandLine.Option(names = "--accelerator-type", description = "Type of this accelerator.")
    private String type;

    @CommandLine.Option(
        names = "--accelerator-core-count",
        description = "Count of cores of this accelerator.")
    private Long coreCount;
  }

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
}
