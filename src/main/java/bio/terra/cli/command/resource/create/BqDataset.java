package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetLifetime;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.workspace.model.StewardshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import scala.Predef.StringFormat;

/** This class corresponds to the fourth-level "terra resource create bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a controlled BigQuery dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(BqDataset.class);
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin BqDatasetLifetime bqDatasetLifetimeOptions;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "BigQuery dataset id.")
  private String bigQueryDatasetId;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1",
      description = "Dataset location (https://cloud.google.com/bigquery/docs/locations).")
  private String location;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a controlled BigQuery dataset to the workspace. */
  @Override
  protected void execute() {
    logger.debug("terra resource create bq-dataset");
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateBqDatasetParams.Builder createParams =
        new CreateBqDatasetParams.Builder()
            .resourceFields(createResourceParams.build())
            .datasetId(bigQueryDatasetId)
            .location(location)
            .defaultPartitionLifetimeSeconds(
                bqDatasetLifetimeOptions.getDefaultPartitionLifetimeSeconds())
            .defaultTableLifetimeSeconds(bqDatasetLifetimeOptions.getDefaultTableLifetimeSeconds());

    bio.terra.cli.businessobject.resource.BqDataset createdResource =
        bio.terra.cli.businessobject.resource.BqDataset.createControlled(createParams.build());
    formatOption.printReturnValue(new UFBqDataset(createdResource), BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully added controlled BigQuery dataset.");
    returnValue.print();
  }
}
