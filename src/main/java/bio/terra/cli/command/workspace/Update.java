package bio.terra.cli.command.workspace;

import bio.terra.cli.Context;
import bio.terra.cli.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandWorkspace;
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

  static class UpdateArgGroup {
    @CommandLine.Option(names = "--name", required = false, description = "workspace display name")
    private String displayName;

    @CommandLine.Option(
        names = "--description",
        required = false,
        description = "workspace description")
    private String description;
  }

  @CommandLine.Mixin Format formatOption;

  /** Update the mutable properties of an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspaceToUpdate =
        Context.requireWorkspace().update(argGroup.displayName, argGroup.description);
    formatOption.printReturnValue(
        new CommandWorkspace.Builder(workspaceToUpdate).build(), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(CommandWorkspace returnValue) {
    OUT.println("Workspace successfully updated.");
    returnValue.print();
  }
}
