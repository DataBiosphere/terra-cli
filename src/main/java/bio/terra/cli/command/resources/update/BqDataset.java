package bio.terra.cli.command.resources.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources update bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Update a Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a Big Query dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resources.BqDataset resource =
        Context.requireWorkspace()
            .getResourceOfType(
                resourceUpdateOptions.resourceNameOption.name, Resource.Type.BQ_DATASET);

    // make the update request
    resource.update(resourceUpdateOptions.populateMetadataFields().build());
    formatOption.printReturnValue(new UFBqDataset(resource), BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully updated Big Query dataset.");
    returnValue.print();
  }
}
