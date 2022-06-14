package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.NotebookMetadata;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import picocli.CommandLine;

@CommandLine.Command(
    name = "gcp-notebook",
    description = "Update the gcp notebook.",
    showDefaultValues = true)
public class GcpNotebook extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin NotebookMetadata notebookMetadataOption;

  /** Update a GCP AI notebook in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcpNotebook resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Type.AI_NOTEBOOK);

    resource.updateControlled(
        new UpdateControlledGcpNotebookParams.Builder()
            .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
            .notebookUpdateParameters(notebookMetadataOption.getMetadata())
            .build());
    // re-load the resource so we display all properties with up-to-date values
    resource =
        Context.requireWorkspace().getResource(resource.getName()).castToType(Type.AI_NOTEBOOK);
    formatOption.printReturnValue(new UFGcpNotebook(resource), GcpNotebook::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcpNotebook returnValue) {
    OUT.println("Successfully updated GCP notebook.");
    returnValue.print();
  }
}
