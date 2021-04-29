package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateControlledResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a controlled Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin CreateControlledResource createControlledResourceMixin;

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
    createControlledResourceMixin.validateAccessOptions();
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledBigQueryDataset(
                createControlledResourceMixin.name,
                createControlledResourceMixin.description,
                createControlledResourceMixin.cloning,
                createControlledResourceMixin.access,
                createControlledResourceMixin.privateUserEmail,
                createControlledResourceMixin.privateIamRoles,
                bigQueryDatasetId,
                location);
    formatOption.printReturnValue(resource, BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled Big Query dataset.");
    PrintingUtils.printResource(returnValue);
  }
}
