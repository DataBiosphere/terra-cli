package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.inputs.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.inputs.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a controlled Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "Big Query dataset id")
  private String bigQueryDatasetId;

  @CommandLine.Option(
      names = "--location",
      description = "Dataset location (https://cloud.google.com/bigquery/docs/locations)")
  private String location;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a controlled Big Query dataset to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    controlledResourceCreationOptions.validateAccessOptions();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateBqDatasetParams.Builder createParams =
        new CreateBqDatasetParams.Builder()
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
