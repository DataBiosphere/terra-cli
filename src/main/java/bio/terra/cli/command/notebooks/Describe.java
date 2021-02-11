package bio.terra.cli.command.notebooks;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks describe" command. */
@CommandLine.Command(
    name = "describe",
    description = "Describe an AI Notebook instance within your workspace.")
public class Describe implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "The name of the notebook instance.")
  private String instanceName;

  @CommandLine.Option(
      names = "location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance, by default '${DEFAULT-VALUE}'.")
  private String location;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();
    workspaceContext.requireCurrentWorkspace();

    AuthenticationManager authenticationManager =
        new AuthenticationManager(globalContext, workspaceContext);
    authenticationManager.loginTerraUser();

    String command = "gcloud notebooks instances describe $INSTANCE_NAME --location=$LOCATION";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("INSTANCE_NAME", instanceName);
    envVars.put("LOCATION", location);

    // TODO(wchamber): Consider reformatting the ouptut or otherwise highlighting the proxy uri.
    new DockerAppsRunner(globalContext, workspaceContext)
        .runToolCommand(
            command, /* workingDir =*/ null, envVars, /* bindMounts =*/ new HashMap<>());

    return 0;
  }
}
