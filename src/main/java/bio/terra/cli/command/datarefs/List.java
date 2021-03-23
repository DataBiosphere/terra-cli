package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs list" command. */
@CommandLine.Command(name = "list", description = "List all data references.")
public class List extends CommandSetup {

  @CommandLine.Mixin FormatFlag formatFlag;

  /** List the data references in the workspace. */
  @Override
  protected void execute() {
    java.util.List<CloudResource> listDataRefsReturnValue =
        new WorkspaceManager(globalContext, workspaceContext).listDataReferences();
    formatFlag.printReturnValue(listDataRefsReturnValue, List::printText);
  }

  /**
   * Print this command's output in text format.
   *
   * @param returnValue command return value object
   */
  private static void printText(java.util.List<CloudResource> returnValue) {
    for (CloudResource dataReference : returnValue) {
      out.println(dataReference.name + " (" + dataReference.type + "): " + dataReference.cloudId);
    }
  }
}
