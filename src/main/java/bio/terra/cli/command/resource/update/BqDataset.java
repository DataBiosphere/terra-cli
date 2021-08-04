package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetExpiration;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Update a BigQuery dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin BqDatasetExpiration bqDatasetExpiration;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a BigQuery dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined() && !bqDatasetExpiration.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.BqDataset resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.BQ_DATASET);

    if (resource.getStewardshipType().equals(StewardshipType.REFERENCED)) {
      if (bqDatasetExpiration.isDefined()) {
        throw new UserActionableException(
            "Expiration time can only be updated for controlled resources.");
      }
      resource.updateReferenced(resourceUpdateOptions.populateMetadataFields().build());
    } else {
      resource.updateControlled(
          new UpdateBqDatasetParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .partitionExpirationTime(bqDatasetExpiration.getPartitionExpiration())
              .tableExpirationTime(bqDatasetExpiration.getTableExpiration())
              .build());
    }

    formatOption.printReturnValue(new UFBqDataset(resource), BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully updated BigQuery dataset.");
    returnValue.print();
  }
}
