package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.service.GoogleNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

// TODO-Dex
/** This class corresponds to the third-level "terra notebook start" command. */
@CommandLine.Command(
    name = "start",
    description = "Start a stopped GCP Notebook instance within your workspace.",
    showDefaultValues = true)
public class Start extends BaseCommand {
  @CommandLine.Mixin NotebookInstance instanceOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    InstanceName instanceName = instanceOption.toInstanceName();
    GoogleNotebooks notebooks = new GoogleNotebooks(Context.requireUser().getPetSACredentials());
    notebooks.start(instanceName);
    OUT.println("Notebook instance starting. It may take a few minutes before it is available");
  }
}
