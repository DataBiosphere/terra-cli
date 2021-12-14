package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetLifetime;
import bio.terra.cli.command.shared.options.BqDatasetNewIds;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.controlled.UpdateControlledBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.referenced.UpdateReferencedBqDatasetParams;
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
  @CommandLine.Mixin BqDatasetLifetime bqDatasetLifetimeOptions;
  @CommandLine.Mixin BqDatasetNewIds bqDatasetNewIds;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a BigQuery dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !bqDatasetLifetimeOptions.isDefined()
        && !bqDatasetNewIds.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.BqDataset resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.BQ_DATASET);

    if (resource.getStewardshipType().equals(StewardshipType.REFERENCED)) {
      if (bqDatasetLifetimeOptions.isDefined()) {
        throw new UserActionableException(
            "Default lifetime can only be updated for controlled resources.");
      }
      UpdateReferencedBqDatasetParams.Builder updateParams =
          new UpdateReferencedBqDatasetParams.Builder()
              .resourceParams(resourceUpdateOptions.populateMetadataFields().build())
              .datasetId(bqDatasetNewIds.getNewBqDatasetId().orElse(resource.getDatasetId()))
              .projectId(bqDatasetNewIds.getNewGcpProjectId().orElse(resource.getProjectId()));
      resource.updateReferenced(updateParams.build());
    } else {
      resource.updateControlled(
          new UpdateControlledBqDatasetParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .defaultPartitionLifetimeSeconds(
                  bqDatasetLifetimeOptions.getDefaultPartitionLifetimeSeconds())
              .defaultTableLifetimeSeconds(
                  bqDatasetLifetimeOptions.getDefaultTableLifetimeSeconds())
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
