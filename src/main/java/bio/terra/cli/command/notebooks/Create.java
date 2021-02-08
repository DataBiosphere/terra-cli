package bio.terra.cli.command.notebooks;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.DockerToolsManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.TerraUser;
import bio.terra.cli.model.WorkspaceContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks create" command. */
@CommandLine.Command(
    name = "create",
    description = "Create a new AI Notebook instance within your workspace.")
public class Create implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description =
          "The unique name to give to the notebook instance. Cannot be changed later. "
              + "The instance name must be 1 to 63 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash.")
  private String instanceName;

  @CommandLine.Option(
      names = "location",
      defaultValue = "us-central1-a",
      description =
          "The Google Cloud location to create the instance within, by default '${DEFAULT-VALUE}'.")
  private String location;

  @CommandLine.Option(
      names = "machine-type",
      defaultValue = "n1-standard-4",
      description = "The Compute Engine machine type of this instance.")
  private String machineType;

  @CommandLine.Option(
      names = "vm-image-project",
      defaultValue = "deeplearning-platform-release",
      description = "The ID of the Google Cloud project that this VM image belongs to.")
  private String vmImageProject;

  @CommandLine.Option(
      names = "vm-image-family",
      defaultValue = "tf-latest-gpu",
      description =
          "Use this VM image family to find the image; the newest image in this family will be used.")
  private String vmImageFamily;

  // TODO: Add boot disk types & gpu size configs.

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();
    workspaceContext.requireCurrentWorkspace();

    AuthenticationManager authenticationManager =
        new AuthenticationManager(globalContext, workspaceContext);
    authenticationManager.loginTerraUser();
    TerraUser user = globalContext.requireCurrentTerraUser();
    String projectId = workspaceContext.getGoogleProject();

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
            + "--metadata=^:^installed-extensions=jupyterlab_bigquery-latest.tar.gz,jupyterlab_gcsfilebrowser-latest.tar.gz,jupyterlab_gcpscheduler-latest.tar.gz";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("INSTANCE_NAME", instanceName);
    envVars.put("LOCATION", location);
    envVars.put("USER_EMAIL", user.terraUserName);
    envVars.put("SERVICE_ACCOUNT", user.petSACredentials.getClientEmail());
    envVars.put("MACHINE_TYPE", machineType);
    envVars.put("VM_IMAGE_PROJECT", vmImageProject);
    envVars.put("VM_IMAGE_FAMILY", vmImageFamily);
    // 'network' is the name of the VPC network instance created by the Buffer Service.
    // Instead of hard coding this, we could try to look up the name of the network on the project.
    envVars.put("NETWORK", "projects/" + projectId + "/global/networks/network");
    // Assume the zone is related to the location like 'us-west1' is to 'us-west1-b'.
    String zone = location.substring(0, location.length() - 2);
    // Like 'network', 'subnetwork is the name of the subnetwork created by the Buffer Service in
    // each zone.
    envVars.put("SUBNET", "projects/" + projectId + "/regions/" + zone + "/subnetworks/subnetwork");

    // TODO(PF-434): Stream back the docker container output.
    String logs =
        new DockerToolsManager(globalContext, workspaceContext)
            .runToolCommand(
                command, /* workingDir =*/ null, envVars, /* bindMounts =*/ new HashMap<>());

    System.out.println(logs);
    System.out.println(
        "Notebook instance starting. This will take ~5-10 minutes.\n"
            + "See your notebooks in this workspace at https://console.cloud.google.com/ai-platform/notebooks/list/instances?project="
            + projectId);
    return 0;
  }
}
