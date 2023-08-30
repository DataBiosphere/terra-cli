package bio.terra.cli.command.folder;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.FolderId;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.UFFolder;
import bio.terra.workspace.model.Folder;
import java.util.UUID;
import org.apache.http.util.TextUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra folder update" command. */
@Command(name = "update", description = "Update a folder.")
public class Update extends BaseCommand {

  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin FolderId folderId;

  @CommandLine.Option(names = "--new-name", description = "New display name of the folder")
  private String displayName;

  @CommandLine.Option(names = "--new-description", description = "Description name of the folder")
  private String description;

  @CommandLine.Option(
      names = "--new-parent-folder-id",
      description = "Id of the new parent folder id")
  private UUID parentFolderId;

  @CommandLine.Option(
      names = "--move-to-root",
      description = "Remove all of its parents and make it into a root folder")
  private boolean moveToRoot;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    if (TextUtils.isEmpty(displayName)
        && TextUtils.isEmpty(description)
        && parentFolderId == null
        && !moveToRoot) {
      throw new UserActionableException(
          "new-name, new-description, new-parent-folder-id or move-to-root must be specified.");
    }
    if (parentFolderId != null && moveToRoot) {
      throw new UserActionableException(
          "received conflicting input. move-to-root and new-parent-folder-id cannot both be set");
    }
    Folder folder =
        Context.requireWorkspace()
            .updateFolder(folderId.folderId, displayName, description, parentFolderId, moveToRoot);
    formatOption.printReturnValue(new UFFolder(folder), this::printText);
  }

  private void printText(UFFolder returnValue) {
    OUT.println("Successfully updated a folder.");
    returnValue.print();
  }
}
