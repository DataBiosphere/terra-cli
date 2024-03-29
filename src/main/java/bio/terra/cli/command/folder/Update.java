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
import org.apache.commons.lang3.StringUtils;
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

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ParentFolderIdArgGroup argGroup;

  static class ParentFolderIdArgGroup {
    @CommandLine.Option(
        names = "--new-parent-folder-id",
        description = "ID of the new parent folder")
    private UUID parentFolderId;

    @CommandLine.Option(
        names = "--move-to-root",
        description = "Remove all of its parents and make it into a root folder")
    private boolean moveToRoot;
  }

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    if (StringUtils.isEmpty(displayName)
        && StringUtils.isEmpty(description)
        && argGroup.parentFolderId == null
        && !argGroup.moveToRoot) {
      throw new UserActionableException(
          "new-name, new-description, new-parent-folder-id or move-to-root must be specified.");
    }

    Folder folder =
        Context.requireWorkspace()
            .updateFolder(
                folderId.folderId,
                displayName,
                description,
                argGroup.parentFolderId,
                argGroup.moveToRoot);
    formatOption.printReturnValue(new UFFolder(folder), this::printText);
  }

  private void printText(UFFolder returnValue) {
    OUT.println("Successfully updated a folder.");
    returnValue.print();
  }
}
