package bio.terra.cli.command.resource;

import static bio.terra.cli.command.resource.ListTree.TERRA_FOLDER_ID_PROPERTY_KEY;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import java.util.Map;
import java.util.UUID;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource move" command. */
@CommandLine.Command(name = "move", description = "move resource to a folder.")
public class MoveToFolder extends WsmBaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Option(names = "--folder-id", required = true, description = "folder id")
  public UUID folderId;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    var properties = Map.of(TERRA_FOLDER_ID_PROPERTY_KEY, folderId.toString());
    var updatedResource =
        Context.requireWorkspace().updateResourceProperties(resource.getId(), properties);

    formatOption.printReturnValue(updatedResource.serializeToCommand(), UFResource::print);
  }
}
