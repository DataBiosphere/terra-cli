package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CreateResource;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateBqDataset;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a referenced Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin CreateResource createResourceOptions;

  @CommandLine.Option(names = "--project-id", required = true, description = "GCP project id")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "Big Query dataset id")
  private String bigQueryDatasetId;

  @CommandLine.Mixin Format formatOption;

  /** Add a referenced Big Query dataset to the workspace. */
  @Override
  protected void execute() {
    // build the resource object to add
    CreateUpdateResource.Builder createResourceParams =
        createResourceOptions.populateMetadataFields().stewardshipType(StewardshipType.REFERENCED);
    CreateUpdateBqDataset.Builder createParams =
        new CreateUpdateBqDataset.Builder()
            .resourceFields(createResourceParams.build())
            .projectId(gcpProjectId)
            .datasetId(bigQueryDatasetId);

    bio.terra.cli.businessobject.resources.BqDataset createdResource =
        bio.terra.cli.businessobject.resources.BqDataset.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFBqDataset(createdResource), BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully added referenced Big Query dataset.");
    returnValue.print();
  }
}
