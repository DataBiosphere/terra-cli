package bio.terra.cli.command.notebooks;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.NotebookName;
import bio.terra.cli.service.utils.GoogleAiNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks stop" command. */
@CommandLine.Command(
    name = "stop",
    description = "Stop a running AI Notebook instance within your workspace.",
    showDefaultValues = true)
public class Stop extends BaseCommand {

  @CommandLine.Mixin NotebookName nameOption;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    InstanceName instanceName = nameOption.toInstanceName(globalContext, workspaceContext);
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(globalContext.requireCurrentTerraUser().userCredentials);
    notebooks.stop(instanceName);
    OUT.println("Notebook instance stopped");
  }
}
