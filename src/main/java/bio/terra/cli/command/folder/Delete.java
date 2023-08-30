package bio.terra.cli.command.folder;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.FolderId;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra folder delete" command. */
@Command(name = "delete", description = "Delete a folder.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Mixin FolderId folderId;

  /** Create a spend profile. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Context.requireWorkspace().deleteFolder(folderId.folderId);
    OUT.println("Folder successfully deleted.");
  }
}
