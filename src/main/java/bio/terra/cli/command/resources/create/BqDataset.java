package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CreateControlledResource;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateBqDataset;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a controlled Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin CreateControlledResource createControlledResourceOptions;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "Big Query dataset id")
  private String bigQueryDatasetId;

  @CommandLine.Option(
      names = "--location",
      description = "Dataset location (https://cloud.google.com/storage/docs/locations)")
  private String location;

  @CommandLine.Mixin Format formatOption;

  /** Add a controlled Big Query dataset to the workspace. */
  @Override
  protected void execute() {
    createControlledResourceOptions.validateAccessOptions();

    // build the resource object to create
    CreateUpdateResource.Builder createResourceParams =
        createControlledResourceOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateUpdateBqDataset.Builder createParams =
        new CreateUpdateBqDataset.Builder()
            .resourceFields(createResourceParams.build())
            .datasetId(bigQueryDatasetId)
            .location(location);

    bio.terra.cli.businessobject.resources.BqDataset createdResource =
        bio.terra.cli.businessobject.resources.BqDataset.createControlled(createParams.build());
    formatOption.printReturnValue(new UFBqDataset(createdResource), BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully added controlled Big Query dataset.");
    returnValue.print();
  }
}
