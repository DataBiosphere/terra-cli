package bio.terra.cli.command.notebooks;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.NotebookInstance;
import bio.terra.cli.service.utils.GoogleAiNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks start" command. */
@CommandLine.Command(
    name = "start",
    description = "Start a stopped AI Notebook instance within your workspace.",
    showDefaultValues = true)
public class Start extends BaseCommand {

  @CommandLine.Mixin NotebookInstance instanceOption;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    InstanceName instanceName = instanceOption.toInstanceName(globalContext, workspaceContext);
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(globalContext.requireCurrentTerraUser().userCredentials);
    notebooks.start(instanceName);
    OUT.println("Notebook instance starting. It may take a few minutes before it is available");
  }
}
