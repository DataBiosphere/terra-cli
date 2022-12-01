package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.service.GoogleNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebook stop" command. */
@CommandLine.Command(
    name = "stop",
    description = "Stop a running Notebook instance within your workspace.",
    showDefaultValues = true)
public class Stop extends BaseCommand {
  @CommandLine.Mixin NotebookInstance instanceOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    InstanceName instanceName = instanceOption.toInstanceName();
    GoogleNotebooks notebooks = new GoogleNotebooks(Context.requireUser().getPetSACredentials());
    notebooks.stop(instanceName);
    OUT.println("Notebook instance stopped");
  }
}
