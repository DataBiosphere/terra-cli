package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources list" command. */
@Command(name = "list", description = "List all controlled resources.")
public class List extends BaseCommand {

  @CommandLine.Mixin FormatOption formatOption;

  /** List all controlled resources. */
  @Override
  protected void execute() {
    java.util.List<CloudResource> resources =
        new WorkspaceManager(globalContext, workspaceContext).listResources();
    formatOption.printReturnValue(resources, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<CloudResource> returnValue) {
    for (CloudResource resource : returnValue) {
      OUT.println(resource.name + " (" + resource.type + "): " + resource.cloudId);
    }
  }
}
