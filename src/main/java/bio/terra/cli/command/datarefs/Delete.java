package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs delete" command. */
@CommandLine.Command(name = "delete", description = "Delete an existing data reference.")
public class Delete extends CommandSetup {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatFlag formatFlag;

  /** Delete a data reference from the workspace. */
  @Override
  protected void execute() {
    CloudResource deleteDataRefReturnValue =
        new WorkspaceManager(globalContext, workspaceContext).deleteDataReference(name);
    formatFlag.printReturnValue(deleteDataRefReturnValue, Delete::printText);
  }

  /**
   * Print this command's output in text format.
   *
   * @param returnValue command return value object
   */
  private static void printText(CloudResource returnValue) {
    OUT.println("Workspace data reference successfully deleted: " + returnValue.name);
  }
}
