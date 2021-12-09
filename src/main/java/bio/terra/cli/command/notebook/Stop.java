package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.service.GoogleNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebook stop" command. */
@CommandLine.Command(
    name = "stop",
    description = "Stop a running GCP Notebook instance within your workspace.",
    showDefaultValues = true)
public class Stop extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Stop.class);

  @CommandLine.Mixin NotebookInstance instanceOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    logger.debug("terra notebook stop");
    workspaceOption.overrideIfSpecified();
    InstanceName instanceName = instanceOption.toInstanceName();
    GoogleNotebooks notebooks = new GoogleNotebooks(Context.requireUser().getPetSACredentials());
    notebooks.stop(instanceName);
    OUT.println("Notebook instance stopped");
  }
}
