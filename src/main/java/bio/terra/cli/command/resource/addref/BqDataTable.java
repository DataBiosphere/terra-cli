package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateBqDataTableParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Add a referenced BigQuery Data Table.",
    showDefaultValues = true)
public class BqDataTable extends BaseCommand {
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;

  @CommandLine.Option(
      names = "--project-id",
      required = true,
      description = "GCP project id of the data table.")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "BigQuery dataset id.")
  private String bigQueryDatasetId;

  @CommandLine.Option(
      names = "--table-id",
      required = true,
      description = "BigQuery data table id.")
  private String bigQueryDataTableId;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced BigQuery DataTable to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParamsBuilder =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.REFERENCED);
    CreateBqDataTableParams.Builder createParamsBuilder =
        new CreateBqDataTableParams.Builder()
            .resourceFields(createResourceParamsBuilder.build())
            .projectId(gcpProjectId)
            .datasetId(bigQueryDatasetId)
            .dataTableId(bigQueryDataTableId);

    bio.terra.cli.businessobject.resource.BqDataTable createdResource =
        bio.terra.cli.businessobject.resource.BqDataTable.addReferenced(
            createParamsBuilder.build());
    formatOption.printReturnValue(new UFBqDataTable(createdResource), BqDataTable::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataTable returnValue) {
    OUT.println("Successfully added referenced BigQuery data table.");
    returnValue.print();
  }
}
