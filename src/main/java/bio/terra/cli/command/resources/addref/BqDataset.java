package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a referenced Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Option(names = "--description", description = "Description of the resource")
  private String description;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}")
  private CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;

  @CommandLine.Option(names = "--project-id", required = true, description = "GCP project id")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "Big Query dataset id")
  private String bigQueryDatasetId;

  @CommandLine.Mixin FormatOption formatOption;

  /** Add a referenced Big Query dataset to the workspace. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext)
            .createReferencedBigQueryDataset(
                name, description, cloning, gcpProjectId, bigQueryDatasetId);
    formatOption.printReturnValue(resource, BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added referenced Big Query dataset.");
    bio.terra.cli.command.resources.Describe.printText(returnValue);
  }
}
