package bio.terra.cli.command.cluster;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DataprocClusterName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.AxonServerService;
import bio.terra.workspace.model.CloudPlatform;
import java.util.UUID;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra cluster start" command. */
@CommandLine.Command(
    name = "start",
    description = "Start a stopped Dataproc cluster within your workspace.",
    showDefaultValues = true)
public class Start extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin DataprocClusterName dataprocClusterName;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace workspace = Context.requireWorkspace();

    if (workspace.getCloudPlatform() == CloudPlatform.GCP) {
      UUID resourceId = dataprocClusterName.toClusterResourceId();
      AxonServerService.fromContext().startCluster(workspace.getUuid(), resourceId);
    } else {
      throw new UserActionableException(
          "Clusters not supported on workspace cloud platform " + workspace.getCloudPlatform());
    }

    OUT.println("Cluster starting. It may take a few minutes before it is available.");
  }
}
