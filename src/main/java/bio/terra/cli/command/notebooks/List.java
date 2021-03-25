package bio.terra.cli.command.notebooks;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks describe" command. */
@CommandLine.Command(
    name = "list",
    description = "List the AI Notebook instance within your workspace for the specified location.",
    showDefaultValues = true)
public class List extends BaseCommand {
  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance.")
  private String location;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    String command = "gcloud notebooks instances list --location=$LOCATION";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("LOCATION", location);

    // TODO(wchamber): Output more relevant information, like the proxy uri.
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(command, envVars);
  }
}
