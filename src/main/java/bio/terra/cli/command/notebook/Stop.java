package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.cloud.gcp.GoogleNotebooks;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.CloudPlatform;
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
    Workspace workspace = Context.requireWorkspace();

    if (workspace.getCloudPlatform() == CloudPlatform.GCP) {
      InstanceName instanceName = instanceOption.toGcpNotebookInstanceName();
      GoogleNotebooks notebooks = new GoogleNotebooks(Context.requireUser().getPetSACredentials());
      notebooks.stop(instanceName);

    } else if (workspace.getCloudPlatform() == CloudPlatform.AWS) {
      AwsSageMakerNotebook awsNotebook = instanceOption.toAwsNotebookResource();
      WorkspaceManagerServiceAws.fromContext()
          .stopSageMakerNotebook(workspace.getUuid(), awsNotebook);

    } else {
      throw new UserActionableException(
          "Notebooks not supported on workspace cloud platform " + workspace.getCloudPlatform());
    }

    OUT.println("Notebook instance stopped");
  }
}
