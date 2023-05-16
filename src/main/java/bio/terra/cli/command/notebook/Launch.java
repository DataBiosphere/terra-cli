package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.workspace.model.CloudPlatform;
import java.net.URL;
import org.json.JSONObject;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebook launch" command. */
@CommandLine.Command(
    name = "launch",
    description = "Launch a running Notebook instance within your workspace.",
    showDefaultValues = true)
public class Launch extends BaseCommand {
  private static final String PROXY_URL = "proxy_url";
  @CommandLine.Mixin NotebookInstance instanceOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--proxy-view",
      description = "AWS Sagemaker Notebook view to be launched: ${COMPLETION-CANDIDATES}.",
      defaultValue = "JUPYTER",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
  private AwsSageMakerNotebook.ProxyView proxyView;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace workspace = Context.requireWorkspace();

    if (workspace.getCloudPlatform() == CloudPlatform.GCP) {
      throw new UserActionableException("Launch notebooks not implemented for GCP Notebooks");

    } else if (workspace.getCloudPlatform() == CloudPlatform.AWS) {
      AwsSageMakerNotebook awsNotebook = instanceOption.toAwsNotebookResource();
      URL proxyUrl =
          WorkspaceManagerServiceAws.fromContext()
              .getSageMakerNotebookProxyUrl(workspace.getUuid(), awsNotebook, proxyView);

      JSONObject object = new JSONObject();
      object.put(PROXY_URL, proxyUrl.toString());
      formatOption.printReturnValue(object, this::printText, this::printJson);

    } else {
      throw new UserActionableException(
          "Notebooks not supported on workspace cloud platform " + workspace.getCloudPlatform());
    }
  }

  private void printText(JSONObject object) {
    OUT.println(object.get(PROXY_URL));
  }

  private void printJson(JSONObject object) {
    OUT.println(object);
  }
}
