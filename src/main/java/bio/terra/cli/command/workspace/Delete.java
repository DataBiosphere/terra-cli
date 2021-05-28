package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Workspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Delete an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspaceToDelete = GlobalContext.get().requireCurrentWorkspace();
    workspaceToDelete.delete();
    formatOption.printReturnValue(workspaceToDelete, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(Workspace returnValue) {
    OUT.println("Workspace successfully deleted.");
    returnValue.printText();
  }
}
