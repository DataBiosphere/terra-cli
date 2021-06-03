package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Delete an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspaceToDelete = Context.requireWorkspace();
    workspaceToDelete.delete();
    formatOption.printReturnValue(new CommandWorkspace(workspaceToDelete), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(CommandWorkspace returnValue) {
    OUT.println("Workspace successfully deleted.");
    returnValue.print();
  }
}
