package bio.terra.cli.command.notebooks;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.DockerToolsManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks describe" command. */
@CommandLine.Command(
    name = "list",
    description = "List the AI Notebook instance within your workspace for the specified location.")
public class List implements Callable<Integer> {
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

    String command = "gcloud notebooks instances list --location=$LOCATION";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("LOCATION", location);

    // TODO(wchamber): Output more relevant information, like the proxy uri.
    new DockerToolsManager(globalContext, workspaceContext)
        .runToolCommand(
            command, /* workingDir =*/ null, envVars, /* bindMounts =*/ new HashMap<>());

    return 0;
  }
}
