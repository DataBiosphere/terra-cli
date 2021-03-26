package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.WorkspaceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace update" command. */
@Command(name = "update", description = "Update an existing workspace.")
public class Update extends BaseCommand {

  @CommandLine.ArgGroup(exclusive = false, multiplicity = "1..2")
  Update.UpdateArgGroup argGroup;

  static class UpdateArgGroup {
    @CommandLine.Option(names = "--name", required = false, description = "workspace display name")
    private String displayName;

    @CommandLine.Option(
        names = "--description",
        required = false,
        description = "workspace description")
    private String description;
  }

  @CommandLine.Mixin FormatOption formatOption;

  /** Update the mutable properties of an existing workspace. */
  @Override
  protected void execute() {
    new WorkspaceManager(globalContext, workspaceContext)
        .updateWorkspace(argGroup.displayName, argGroup.description);
    formatOption.printReturnValue(workspaceContext.terraWorkspaceModel, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(WorkspaceDescription returnValue) {
    OUT.println("Workspace successfully updated: " + workspaceContext.getWorkspaceId());
  }
}
