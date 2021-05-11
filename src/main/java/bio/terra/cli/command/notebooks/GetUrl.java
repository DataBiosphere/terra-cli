package bio.terra.cli.command.notebooks;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.NotebookName;
import bio.terra.cli.service.utils.GoogleAiNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import com.google.api.services.notebooks.v1.model.Instance;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks get-url" command. */
@CommandLine.Command(
    name = "get-url",
    description = "Get the proxy URL of an AI Notebook instance running within your workspace.",
    showDefaultValues = true)
public class GetUrl extends BaseCommand {

  @CommandLine.Mixin NotebookName nameOption;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    InstanceName instanceName = nameOption.toInstanceName(globalContext, workspaceContext);
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(globalContext.requireCurrentTerraUser().userCredentials);
    Instance instance = notebooks.get(instanceName);
    String proxyUri = instance.getProxyUri();
    if (proxyUri != null) {
      OUT.println("Proxy url: " + proxyUri);
    } else {
      OUT.println("No proxy url available. Notebook state: " + instance.getState());
    }
  }
}
