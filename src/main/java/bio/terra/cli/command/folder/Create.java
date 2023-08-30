package bio.terra.cli.command.folder;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFFolder;
import bio.terra.workspace.model.Folder;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra folder create" command. */
@Command(name = "create", description = "Create a folder.")
public class Create extends BaseCommand {

  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(names = "--name", required = true, description = "Display name of the folder")
  private String displayName;

  @CommandLine.Option(
      names = "--properties",
      split = ",",
      description =
          "Folder properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  private Map<String, String> folderProperties;

  @CommandLine.Option(names = "--description", description = "Description name of the folder")
  private String description;

  @CommandLine.Option(
      names = "--parent-folder-id",
      description = "Id of the parent folder, if not set, create a folder under root")
  private UUID parentFolderId;

  /** Create a spend profile. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Folder folder =
        Context.requireWorkspace()
            .createFolder(displayName, description, parentFolderId, folderProperties);
    formatOption.printReturnValue(new UFFolder(folder), this::printText);
  }

  private void printText(UFFolder returnValue) {
    OUT.println("Successfully created a folder.");
    returnValue.print();
  }
}
