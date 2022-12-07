package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetsIds;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddBqTableParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Add a referenced BigQuery Data Table.",
    showDefaultValues = true)
public class BqTable extends WsmBaseCommand {
  @CommandLine.Mixin ReferencedResourceCreation referencedResourceCreationOptions;
  @CommandLine.Mixin BqDatasetsIds bqDatasetsIds;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(names = "--table-id", description = "BigQuery data table id.")
  private String bigQueryTableId;

  @CommandLine.Option(
      names = "--path",
      description = "Path of the big query table (e.g. 'project_id.dataset_id.table_id').")
  public String path;

  /** Print this command's output in text format. */
  private static void printText(UFBqTable returnValue) {
    OUT.println("Successfully added referenced BigQuery data table.");
    returnValue.print();
  }

  /** Add a referenced BigQuery DataTable to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    String datasetId = bqDatasetsIds.getBigQueryDatasetId();
    String projectId = bqDatasetsIds.getGcpProjectId();

    // parsing the path as project id, database id and table id
    if (path != null) {
      if (datasetId != null || projectId != null || bigQueryTableId != null) {
        throw new UserActionableException(
            "Specify either --path or all of --project-id, --dataset-id and --table-id.");
      }
      String[] parsePath = path.split("[.]");
      if (parsePath.length != 3) {
        throw new UserActionableException(
            "Specify a legal path, like 'project_id.dataset_id.table_id'.");
      }
      projectId = parsePath[0];
      datasetId = parsePath[1];
      bigQueryTableId = parsePath[2];
    } else {
      if (datasetId == null || projectId == null || bigQueryTableId == null) {
        throw new UserActionableException("Specify at least one path to add.");
      }
    }

    // build the resource object to add
    CreateResourceParams.Builder createResourceParamsBuilder =
        referencedResourceCreationOptions.populateMetadataFields();
    AddBqTableParams.Builder createParamsBuilder =
        new AddBqTableParams.Builder()
            .resourceFields(createResourceParamsBuilder.build())
            .projectId(projectId)
            .datasetId(datasetId)
            .dataTableId(bigQueryTableId);

    bio.terra.cli.businessobject.resource.BqTable createdResource =
        bio.terra.cli.businessobject.resource.BqTable.addReferenced(createParamsBuilder.build());
    formatOption.printReturnValue(new UFBqTable(createdResource), BqTable::printText);
  }
}
