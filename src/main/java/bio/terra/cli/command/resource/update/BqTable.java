package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetNewIds;
import bio.terra.cli.command.shared.options.CloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqTableParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Update a BigQuery data table.",
    showDefaultValues = true)
public class BqTable extends WsmBaseCommand {
  @CommandLine.Mixin BqDatasetNewIds bqDatasetNewIds;
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin CloningInstructionsForUpdate newCloningInstructionsOption;

  @CommandLine.Option(names = "--new-table-id", description = "New BigQuery table id.")
  private String newBqTableId;

  @CommandLine.Option(
      names = "--new-path",
      description = "New path of the big query table (e.g. 'project_id.dataset_id.table_id').")
  public String newPath;

  /** Print this command's output in text format. */
  private static void printText(UFBqTable returnValue) {
    OUT.println("Successfully updated BigQuery data table.");
    returnValue.print();
  }

  /** Update a BigQuery dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    String newDatasetId = bqDatasetNewIds.getNewBqDatasetId();
    String newProjectId = bqDatasetNewIds.getNewGcpProjectId();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !bqDatasetNewIds.isDefined()
        && newBqTableId == null
        && newPath == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // parsing the path as project id, database id and table id
    if (newPath != null) {
      if (newProjectId != null || newDatasetId != null || newBqTableId != null) {
        throw new UserActionableException(
            "No need to specify --new-project-id and/or --new-dataset-id and/or --new-table-id when --new-path is specified.");
      }
      String[] parsePath = newPath.split("[.]");
      if (parsePath.length != 3) {
        throw new UserActionableException(
            "Specify a legal path, like 'project_id.dataset_id.table_id'.");
      }
      newProjectId = parsePath[0];
      newDatasetId = parsePath[1];
      newBqTableId = parsePath[2];
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.BqTable resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.BQ_TABLE);

    UpdateReferencedBqTableParams.Builder bqTableParams =
        new UpdateReferencedBqTableParams.Builder()
            .resourceParams(resourceUpdateOptions.populateMetadataFields().build())
            .tableId(newBqTableId)
            .datasetId(newDatasetId)
            .projectId(newProjectId)
            .cloningInstructions(newCloningInstructionsOption.getCloning());

    resource.updateReferenced(bqTableParams.build());
    // re-load the resource so we display all properties with up-to-date values
    resource = Context.requireWorkspace().getResource(resource.getName()).castToType(Type.BQ_TABLE);
    formatOption.printReturnValue(new UFBqTable(resource), BqTable::printText);
  }
}
