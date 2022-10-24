package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetLifetime;
import bio.terra.cli.command.shared.options.BqDatasetNewIds;
import bio.terra.cli.command.shared.options.CloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqDatasetParams;
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
  @CommandLine.Mixin CloningInstructionsForUpdate newCloningInstructionsOption;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--new-path",
      description = "New path of the bucket (e.g. 'project_id.dataset_name').")
  public String newPath;

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully updated BigQuery dataset.");
    returnValue.print();
  }

  /** Update a BigQuery dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    String newProjectId = bqDatasetNewIds.getNewGcpProjectId();
    String newDatasetId = bqDatasetNewIds.getNewBqDatasetId();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !bqDatasetLifetimeOptions.isDefined()
        && !bqDatasetNewIds.isDefined()
        && !newCloningInstructionsOption.isDefined()
        && newPath == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // parsing the path as project id and database id.
    if (newPath != null) {
      if (newProjectId != null || newDatasetId != null) {
        throw new UserActionableException(
            "No need to specify --new-project-id and/or --new-dataset-id when --new-path is specified.");
      }
      String[] parsePath = newPath.split("[.]");
      if (parsePath.length != 2) {
        throw new UserActionableException("Specify a legal path, like 'project_id.dataset_id'.");
      }
      newProjectId = parsePath[0];
      newDatasetId = parsePath[1];
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
              .datasetId(newDatasetId)
              .projectId(newProjectId)
              .cloningInstructions(newCloningInstructionsOption.getCloning());
      resource.updateReferenced(updateParams.build());
    } else {
      resource.updateControlled(
          new UpdateControlledBqDatasetParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .defaultPartitionLifetimeSeconds(
                  bqDatasetLifetimeOptions.getDefaultPartitionLifetimeSeconds())
              .defaultTableLifetimeSeconds(
                  bqDatasetLifetimeOptions.getDefaultTableLifetimeSeconds())
              .cloningInstructions(newCloningInstructionsOption.getCloning())
              .build());
    }
    // re-load the resource so we display all properties with up-to-date values
    resource =
        Context.requireWorkspace().getResource(resource.getName()).castToType(Type.BQ_DATASET);
    formatOption.printReturnValue(new UFBqDataset(resource), BqDataset::printText);
  }
}
