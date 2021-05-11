package bio.terra.cli.command.notebooks;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.NotebookName;
import bio.terra.cli.service.utils.GoogleAiNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import com.google.api.services.notebooks.v1.model.Instance;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebooks get-url" command. */
@CommandLine.Command(
    name = "get",
    description = "Get the GCP data of an AI Notebook instance within your workspace.",
    showDefaultValues = true)
public class Get extends BaseCommand {

  @CommandLine.Mixin NotebookName nameOption;

  @CommandLine.Mixin Format formatOption;

  @Override
  protected void execute() {
    workspaceContext.requireCurrentWorkspace();

    InstanceName instanceName = nameOption.toInstanceName(globalContext, workspaceContext);
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(globalContext.requireCurrentTerraUser().userCredentials);
    Instance instance = notebooks.get(instanceName);
    formatOption.printReturnValue(instance, Get::printText);
  }

  /** Print the most interesting subset of the Instance.  */
  private static void printText(Instance instance) {
    OUT.println("Instance name: " + instance.getName());
    OUT.println("State:         " + instance.getState());
    OUT.println("Proxy URL:     " + instance.getProxyUri());
    OUT.println("Create time:   " + instance.getCreateTime());
  }
}
