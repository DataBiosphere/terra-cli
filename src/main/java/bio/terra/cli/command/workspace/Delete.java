package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete extends WsmBaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Delete an existing workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace workspaceToDelete = Context.requireWorkspace();

    // print details about the workspace before showing the delete prompt
    new UFWorkspace(workspaceToDelete).print();
    deletePromptOption.confirmOrThrow();

    workspaceToDelete.delete();
    OUT.println("Workspace successfully deleted.");
  }
}
