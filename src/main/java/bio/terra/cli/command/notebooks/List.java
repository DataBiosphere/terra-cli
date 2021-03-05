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
    name = "list",
    description = "List the AI Notebook instance within your workspace for the specified location.",
    showDefaultValues = true)
public class List implements Callable<Integer> {
  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance.")
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
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(command, envVars);

    return 0;
  }
}
