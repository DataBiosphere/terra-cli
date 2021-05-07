package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateControlledResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.GcpAiNotebookInstanceVmImage;
import bio.terra.workspace.model.PrivateResourceIamRoles;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create ai-notebook" command. */
@CommandLine.Command(
    name = "ai-notebook",
    description = "Add a controlled AI Platform Notebook instance resource.",
    showDefaultValues = true)
public class AiNotebook extends BaseCommand {
  private static final String AUTO_NAME_DATE_FORMAT = "-yyyyMMdd-HHmmss";
  private static final String AUTO_GENERATE_NAME = "{username}" + AUTO_NAME_DATE_FORMAT;
  /** See {@link #mangleUsername(String)}. */
  private static final int MAX_INSTANCE_NAME_LENGTH = 61;

  @CommandLine.Mixin CreateControlledResource createControlledResourceOptions;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "instanceId",
      description =
          "The unique name to give to the notebook instance. Cannot be changed later. "
              + "The instance name must be 1 to 63 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash. If not specified, an "
              + "auto-generated name based on your email address and time will be used.",
      defaultValue = AUTO_GENERATE_NAME)
  private String rawInstanceId;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance.")
  private String location;

  @CommandLine.Option(
      names = "--machine-type",
      defaultValue = "n1-standard-4",
      description = "The Compute Engine machine type of this instance.")
  private String machineType;

  @CommandLine.Option(
      names = "--vm-image-project",
      defaultValue = "deeplearning-platform-release",
      description = "The ID of the Google Cloud project that this VM image belongs to.")
  private String vmImageProject;

  @CommandLine.Option(
      names = "--vm-image-family",
      defaultValue = "r-latest-cpu-experimental",
      description =
          "Use this VM image family to find the image; the newest image in this family will be used.")
  private String vmImageFamily;

  @CommandLine.Option(
      names = "--post-startup-script",
      description =
          "Path to a Bash script that automatically runs after a notebook instance fully boots up. "
              + "The path must be a URL or Cloud Storage path, e.g. 'gs://path-to-file/file-name'")
  private String postStartupScript;

  // TODO: Add boot disk types & gpu size configs.

  @CommandLine.Mixin Format formatOption;

  /** Add a controlled AI Notebook instance to the workspace. */
  @Override
  protected void execute() {
    createControlledResourceOptions.validateAccessOptions();

    var creationParameters =
        new GcpAiNotebookInstanceCreationParameters()
            .instanceId(getInstanceId(globalContext.requireCurrentTerraUser()))
            .location(location)
            .machineType(machineType)
            .vmImage(
                new GcpAiNotebookInstanceVmImage()
                    .projectId(vmImageProject)
                    .imageFamily(vmImageFamily))
            .postStartupScript(postStartupScript)
            .metadata(createMetadata(workspaceContext.getWorkspaceId()));

    ResourceDescription resourceCreated =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledAiNotebookInstance(resourceToCreate(), creationParameters);
    formatOption.printReturnValue(resourceCreated, AiNotebook::printText);
  }

  /** Returns a {@link ResourceDescription} for the resource to be cretaed. */
  private ResourceDescription resourceToCreate() {
    // build the resource object to create
    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (createControlledResourceOptions.privateIamRoles != null
        && !createControlledResourceOptions.privateIamRoles.isEmpty()) {
      privateResourceIamRoles.addAll(createControlledResourceOptions.privateIamRoles);
    }
    return new ResourceDescription()
        .metadata(
            new ResourceMetadata()
                .name(createControlledResourceOptions.name)
                .description(createControlledResourceOptions.description)
                .cloningInstructions(createControlledResourceOptions.cloning)
                .controlledResourceMetadata(
                    new ControlledResourceMetadata()
                        .accessScope(createControlledResourceOptions.access)
                        .privateResourceUser(
                            new PrivateResourceUser()
                                .userName(createControlledResourceOptions.privateUserEmail)
                                .privateResourceIamRoles(privateResourceIamRoles))));
  }

  /** Create the metadata to put on the AI Notebook instance. */
  private Map<String, String> createMetadata(UUID workspaceID) {
    return ImmutableMap.<String, String>builder()

        // The metadata installed-extensions causes the AI Notebooks setup to install some
        // Google JupyterLab extensions. Found by manual inspection of what is created with the
        // cloud console GUI.
        .put(
            "installed-extensions",
            "jupyterlab_bigquery-latest.tar.gz,jupyterlab_gcsfilebrowser-latest.tar.gz,jupyterlab_gcpscheduler-latest.tar.gz")
        // Also set the id of this workspace as metadata on the VM instance.
        .put("terra-workspace-id", workspaceID.toString())
        .build();
  }

  /**
   * Returns the specified instanceId or an auto generated instance name with the username and date
   * time.
   */
  // TODO add some unit tests when we have a testing framework.
  private String getInstanceId(TerraUser user) {
    if (!AUTO_GENERATE_NAME.equals(rawInstanceId)) {
      return rawInstanceId;
    }
    String mangledUsername = mangleUsername(extractUsername(user.terraUserEmail));
    String localDateTimeSuffix =
        DateTimeFormatter.ofPattern(AUTO_NAME_DATE_FORMAT)
            .format(Instant.now().atZone(ZoneId.systemDefault()));
    return mangledUsername + localDateTimeSuffix;
  }

  /**
   * Best effort mangle the user's name so that it meets the requirements for a valid instance name.
   *
   * <p>Instance name id must match the regex '(?:[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?)', i.e. starting
   * with a lowercase alpha character, only alphanumerics and '-' of max length 61. I don't have a
   * documentation link, but gcloud will complain otherwise.
   */
  private static String mangleUsername(String username) {
    // Strip non alpha-numeric or '-' characters.
    String mangledName = username.replaceAll("[^a-zA-Z0-9-]", "");
    if (mangledName.isEmpty()) {
      mangledName = "notebook";
    }
    // Lower case everything, even though only the first character requires lowercase.
    mangledName = mangledName.toLowerCase();
    // Make sure the returned name isn't too long to not have the date time suffix.
    int maxNameLength = MAX_INSTANCE_NAME_LENGTH - AUTO_NAME_DATE_FORMAT.length();
    if (mangledName.length() > maxNameLength) {
      mangledName = mangledName.substring(0, maxNameLength);
    }
    return mangledName;
  }

  private static String extractUsername(String validEmail) {
    return validEmail.substring(0, validEmail.indexOf('@'));
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled AI Notebook instance.");
    PrintingUtils.printText(returnValue);
  }
}
