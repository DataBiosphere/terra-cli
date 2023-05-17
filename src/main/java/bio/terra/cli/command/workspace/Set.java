package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace set" command. */
@Command(name = "set", description = "Set the workspace to an existing one.")
public class Set extends WsmBaseCommand {
  @CommandLine.Mixin Format formatOption;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  WorkspaceIdArgGroup argGroup;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspace =
        argGroup.id != null ? Workspace.load(argGroup.id) : Workspace.load(argGroup.uuid);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully loaded.");
    returnValue.print();
  }

  static class WorkspaceIdArgGroup {
    @CommandLine.Option(names = "--id", description = "Workspace id.")
    // Variable is `id` instead of `userFacingId` because user sees it with `terra workspace set`
    private String id;

    @CommandLine.Option(names = "--uuid", description = "Workspace UUID.")
    private UUID uuid;
  }
}
