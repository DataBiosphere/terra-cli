package bio.terra.cli.command.cluster;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DataprocClusterName;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.AxonServerService;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
import java.util.UUID;
import org.json.JSONObject;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra cluster launch" command. */
@CommandLine.Command(
    name = "launch",
    description = "Launch a Dataproc cluster proxy view within your workspace.",
    showDefaultValues = true)
public class Launch extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin DataprocClusterName dataprocClusterName;

  @CommandLine.Option(
      names = "--proxy-view",
      description =
          "Dataproc cluster component interface proxy view. Available components: ${COMPLETION-CANDIDATES}.",
      defaultValue = "JUPYTER_LAB",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private GcpDataprocCluster.ProxyView proxyView;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace workspace = Context.requireWorkspace();
    CommandUtils.checkDataprocSupport();

    if (workspace.getCloudPlatform() == CloudPlatform.GCP) {
      UUID resourceId = dataprocClusterName.toClusterResourceId();
      bio.terra.axonserver.model.Url proxyUrl =
          AxonServerService.fromContext()
              .getClusterComponentUrl(workspace.getUuid(), resourceId, proxyView.toParam());

      JSONObject object = new JSONObject().put(proxyView.toParam(), proxyUrl.getUrl());
      formatOption.printReturnValue(object, this::printText, this::printJson);
    } else {
      throw new UserActionableException(
          "Clusters not supported on workspace cloud platform " + workspace.getCloudPlatform());
    }
  }

  private void printText(JSONObject object) {
    OUT.println(proxyView.toParam() + ": " + object.get(proxyView.toParam()));
  }
}
