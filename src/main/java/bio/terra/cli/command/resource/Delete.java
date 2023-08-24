package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.CommandUtils;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends WsmBaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    Resource resourceToDelete = Context.requireWorkspace().getResource(resourceNameOption.name);

    if (resourceToDelete.getResourceType().equals(Resource.Type.DATAPROC_CLUSTER)) {
      CommandUtils.checkDataprocSupport();
    }

    // print details about the resource before showing the delete prompt
    resourceToDelete.serializeToCommand().print();
    deletePromptOption.confirmOrThrow();

    resourceToDelete.delete();
    OUT.println("Resource successfully deleted.");
  }
}
