package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs list" command. */
@CommandLine.Command(name = "list", description = "List all data references.")
public class List extends BaseCommand {

  @CommandLine.Mixin FormatOption formatOption;

  /** List the data references in the workspace. */
  @Override
  protected void execute() {
    java.util.List<CloudResource> listDataRefsReturnValue =
        new WorkspaceManager(globalContext, workspaceContext).listDataReferences();
    formatOption.printReturnValue(listDataRefsReturnValue, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<CloudResource> returnValue) {
    for (CloudResource dataReference : returnValue) {
      OUT.println(dataReference.name + " (" + dataReference.type + "): " + dataReference.cloudId);
    }
  }
}
