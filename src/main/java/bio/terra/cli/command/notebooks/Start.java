package bio.terra.cli.command.notebooks;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.service.GoogleAiNotebooks;
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
    InstanceName instanceName = instanceOption.toInstanceName();
    GoogleAiNotebooks notebooks = new GoogleAiNotebooks(Context.requireUser().getUserCredentials());
    notebooks.start(instanceName);
    OUT.println("Notebook instance starting. It may take a few minutes before it is available");
  }
}
