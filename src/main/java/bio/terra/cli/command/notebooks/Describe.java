package bio.terra.cli.command.notebooks;

import bio.terra.cli.apps.AppsRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks describe" command. */
@CommandLine.Command(
    name = "describe",
    description = "Describe an AI Notebook instance within your workspace.",
    showDefaultValues = true)
public class Describe extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "The name of the notebook instance.")
  private String instanceName;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance.")
  private String location;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    String command = "gcloud notebooks instances describe $INSTANCE_NAME --location=$LOCATION";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("INSTANCE_NAME", instanceName);
    envVars.put("LOCATION", location);

    // TODO(wchamber): Consider reformatting the ouptut or otherwise highlighting the proxy uri.
    new AppsRunner(globalContext, workspaceContext).runToolCommand(command, envVars);
  }
}
