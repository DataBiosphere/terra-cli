package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetsIds;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferenceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.AddBqTableParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Add a referenced BigQuery Data Table.",
    showDefaultValues = true)
public class BqTable extends BaseCommand {
  @CommandLine.Mixin ReferenceCreation referenceCreationOptions;

  @CommandLine.Option(
      names = "--table-id",
      required = true,
      description = "BigQuery data table id.")
  private String bigQueryTableId;

  @CommandLine.Mixin BqDatasetsIds bigQueryIds;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced BigQuery DataTable to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParamsBuilder =
        referenceCreationOptions.populateMetadataFields();
    AddBqTableParams.Builder createParamsBuilder =
        new AddBqTableParams.Builder()
            .resourceFields(createResourceParamsBuilder.build())
            .projectId(bigQueryIds.getGcpProjectId())
            .datasetId(bigQueryIds.getBigQueryDatasetId())
            .dataTableId(bigQueryTableId);

    bio.terra.cli.businessobject.resource.BqTable createdResource =
        bio.terra.cli.businessobject.resource.BqTable.addReferenced(createParamsBuilder.build());
    formatOption.printReturnValue(new UFBqTable(createdResource), BqTable::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqTable returnValue) {
    OUT.println("Successfully added referenced BigQuery data table.");
    returnValue.print();
  }
}
