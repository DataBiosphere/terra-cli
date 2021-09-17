package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    UFResource ufResource = resource.serializeToCommand();
    ufResource.print();
    deletePromptOption.confirmOrThrow();
    resource.delete();
    OUT.println("Successfully deleted resource.");
  }
}
