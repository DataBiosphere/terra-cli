package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace update" command. */
@Command(name = "update", description = "Update an existing workspace.")
public class Update extends BaseCommand {

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "1",
      heading = "Property update parameters:%n")
  Update.UpdateArgGroup argGroup;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update the mutable properties of an existing workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace updatedWorkspace =
        Context.requireWorkspace().update(argGroup.id, argGroup.name, argGroup.description);
    updatedWorkspace.listResourcesAndSync();
    formatOption.printReturnValue(new UFWorkspace(updatedWorkspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully updated.");
    returnValue.print();
  }

  static class UpdateArgGroup {
    @CommandLine.Option(names = "--new-id", required = false, description = "Workspace ID.")
    // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace update`
    private String id;

    @CommandLine.Option(
        names = "--new-name",
        required = false,
        description = "Workspace name (not unique).")
    private String name;

    @CommandLine.Option(
        names = "--new-description",
        required = false,
        description = "Workspace description.")
    private String description;
  }
}
