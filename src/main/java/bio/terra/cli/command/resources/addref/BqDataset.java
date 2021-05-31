package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.CreateResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Resource;
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
    bio.terra.cli.context.resources.BqDataset.BqDatasetBuilder resourceToAdd =
        new bio.terra.cli.context.resources.BqDataset.BqDatasetBuilder()
            .projectId(gcpProjectId)
            .datasetId(bigQueryDatasetId);
    resourceToAdd.stewardshipType(StewardshipType.REFERENCED);
    createResourceOptions.populateMetadataFields(resourceToAdd);

    Resource resource = resourceToAdd.build().addOrCreate();
    formatOption.printReturnValue(resource, BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(Resource returnValue) {
    OUT.println("Successfully added referenced Big Query dataset.");
    returnValue.printText();
  }
}
