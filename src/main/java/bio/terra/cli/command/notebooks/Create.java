package bio.terra.cli.command.notebooks;

import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.TerraUser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks create" command. */
@CommandLine.Command(
    name = "create",
    description = "Create a new AI Notebook instance within your workspace.",
    showDefaultValues = true)
public class Create extends BaseCommand {
  private static final String AUTO_NAME_DATE_FORMAT = "-yyyyMMdd-HHmmss";
  private static final String AUTO_GENERATE_NAME = "{username}" + AUTO_NAME_DATE_FORMAT;
  /** See {@link #mangleUsername(String)}. */
  private static final int MAX_INSTANCE_NAME_LENGTH = 61;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "instanceName",
      description =
          "The unique name to give to the notebook instance. Cannot be changed later. "
              + "The instance name must be 1 to 63 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash. If not specified, an "
              + "auto-generated name based on your email address and time will be used.",
      defaultValue = AUTO_GENERATE_NAME)
  private String rawInstanceName;

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

  // TODO: Add boot disk types & gpu size configs.

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    TerraUser user = globalContext.requireCurrentTerraUser();
    String projectId = workspaceContext.getGoogleProject();
    String instanceName = getInstanceName(user);

    // See https://cloud.google.com/sdk/gcloud/reference/notebooks/instances/create
    String command =
        "gcloud notebooks instances create $INSTANCE_NAME "
            + "--location=$LOCATION "
            + "--instance-owners=$USER_EMAIL "
            + "--service-account=$SERVICE_ACCOUNT "
            + "--machine-type=$MACHINE_TYPE "
            + "--vm-image-project=$VM_IMAGE_PROJECT "
            + "--vm-image-family=$VM_IMAGE_FAMILY "
            + "--network=$NETWORK "
            + "--subnet=$SUBNET "
            // The metadata installed-extensions causes the AI Notebooks setup to install some
            // Google JupyterLab extensions. Found by manual inspection of what is created with the
            // cloud console GUI.
            + "--metadata=^:^installed-extensions=jupyterlab_bigquery-latest.tar.gz,jupyterlab_gcsfilebrowser-latest.tar.gz,jupyterlab_gcpscheduler-latest.tar.gz"
            // Also set the id of this workspace as metadata on the VM instance.
            + ":terra-workspace-id=$TERRA_WORKSPACE_ID";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("INSTANCE_NAME", instanceName);
    envVars.put("LOCATION", location);
    envVars.put("USER_EMAIL", user.terraUserEmail);
    envVars.put("SERVICE_ACCOUNT", user.petSACredentials.getClientEmail());
    envVars.put("MACHINE_TYPE", machineType);
    envVars.put("VM_IMAGE_PROJECT", vmImageProject);
    envVars.put("VM_IMAGE_FAMILY", vmImageFamily);
    // 'network' is the name of the VPC network instance created by the Buffer Service.
    // Instead of hard coding this, we could try to look up the name of the network on the project.
    envVars.put("NETWORK", "projects/" + projectId + "/global/networks/network");
    // Assume the zone is related to the location like 'us-west1' is to 'us-west1-b'.
    String zone = location.substring(0, location.length() - 2);
    // Like 'network', 'subnetwork' is the name of the subnetwork created by the Buffer Service in
    // each zone.
    envVars.put("SUBNET", "projects/" + projectId + "/regions/" + zone + "/subnetworks/subnetwork");
    envVars.put("TERRA_WORKSPACE_ID", workspaceContext.getWorkspaceId().toString());

    OUT.println(
        String.format(
            "Creating notebook instance `projects/%s/locations/%s/instanceId/%s`",
            projectId, location, instanceName));
    new DockerCommandRunner(globalContext, workspaceContext).runToolCommand(command, envVars);

    OUT.println(
        "Notebook instance starting. This will take ~5-10 minutes.\n"
            + "See your notebooks in this workspace at https://console.cloud.google.com/ai-platform/notebooks/list/instances?project="
            + projectId);
  }

  /**
   * Returns the specified instanceName or an auto generated instance name with the username and
   * date time.
   */
  // TODO add some unit tests when we have a testing framework.
  private String getInstanceName(TerraUser user) {
    if (!AUTO_GENERATE_NAME.equals(rawInstanceName)) {
      return rawInstanceName;
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
}
