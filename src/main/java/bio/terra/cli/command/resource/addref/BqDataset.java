package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.BqDatasetsIds;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a referenced BigQuery dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin ReferencedResourceCreation referencedResourceCreationOptions;
  @CommandLine.Mixin BqDatasetsIds bigQueryIds;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--dataset-path",
      description = "Path of the dataset (e.g. 'project_id.dataset_id').")
  public String datasetPath;

  /** Print this command's output in text format. */
  private static void printText(UFBqDataset returnValue) {
    OUT.println("Successfully added referenced BigQuery dataset.");
    returnValue.print();
  }

  /** Add a referenced BigQuery dataset to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    String projectId = bigQueryIds.getGcpProjectId();
    String datasetId = bigQueryIds.getBigQueryDatasetId();

    // parsing the path as project id, database id
    if (datasetPath != null) {
      if (projectId != null || datasetId != null) {
        throw new UserActionableException("Specify only one path to add reference.");
      } else {
        String[] parsePath = datasetPath.split("[.]");
        projectId = parsePath[0];
        datasetId = parsePath[1];
      }
    } else {
      if (projectId == null || datasetId == null) {
        throw new UserActionableException("Specify at least one path to update.");
      }
    }

    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        referencedResourceCreationOptions.populateMetadataFields();
    CreateBqDatasetParams.Builder createParams =
        new CreateBqDatasetParams.Builder()
            .resourceFields(createResourceParams.build())
            .projectId(projectId)
            .datasetId(datasetId);

    bio.terra.cli.businessobject.resource.BqDataset createdResource =
        bio.terra.cli.businessobject.resource.BqDataset.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFBqDataset(createdResource), BqDataset::printText);
  }
}
