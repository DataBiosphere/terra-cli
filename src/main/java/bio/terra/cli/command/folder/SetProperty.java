package bio.terra.cli.command.folder;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.FolderId;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFFolder;
import bio.terra.workspace.model.Folder;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra folder set-property" command. */
// This is set-property instead of add-property because this can be used to 1) Add property 2)
// Update existing property.
@Command(name = "set-property", description = "Set the folder properties.")
public class SetProperty extends WsmBaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--properties",
      required = true,
      split = ",",
      description =
          "Folder properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> folderProperties;

  @CommandLine.Mixin FolderId folderId;

  /** Set folder properties. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Folder updatedFolder =
        Context.requireWorkspace().updateFolderProperties(folderId.folderId, folderProperties);
    formatOption.printReturnValue(new UFFolder(updatedFolder), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFFolder folder) {
    OUT.println("Folder properties successfully updated.");
    folder.print();
  }
}
