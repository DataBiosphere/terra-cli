package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceNameDescriptionAndProperty;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(name = "create", description = "Create a new workspace.")
public class Create extends BaseCommand {

  @CommandLine.Option(names = "--id", required = true, description = "Workspace ID")
  // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace create`
  private String id;

  @CommandLine.Mixin WorkspaceNameDescriptionAndProperty workspaceNameAndDescriptionAndProperty;

  @CommandLine.Mixin Format formatOption;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    Workspace workspace =
        Workspace.create(
            id,
            workspaceNameAndDescriptionAndProperty.name,
            workspaceNameAndDescriptionAndProperty.description,
            workspaceNameAndDescriptionAndProperty.properties);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully created.");
    returnValue.print();
  }
}
