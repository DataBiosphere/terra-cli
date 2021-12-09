package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BigQueryIds;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateBqDataTableParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
import bio.terra.workspace.model.StewardshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Add a referenced BigQuery Data Table.",
    showDefaultValues = true)
public class BqDataTable extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(BqDataTable.class);
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;

  @CommandLine.Option(
      names = "--table-id",
      required = true,
      description = "BigQuery data table id.")
  private String bigQueryDataTableId;

  @CommandLine.Mixin BigQueryIds bigQueryIds;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced BigQuery DataTable to the workspace. */
  @Override
  protected void execute() {
    logger.debug("terra resource addref bq-table");
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParamsBuilder =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.REFERENCED);
    CreateBqDataTableParams.Builder createParamsBuilder =
        new CreateBqDataTableParams.Builder()
            .resourceFields(createResourceParamsBuilder.build())
            .projectId(bigQueryIds.getGcpProjectId())
            .datasetId(bigQueryIds.getBigQueryDatasetId())
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
