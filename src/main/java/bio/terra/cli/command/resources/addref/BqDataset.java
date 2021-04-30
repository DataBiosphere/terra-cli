package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Add a referenced Big Query dataset.",
    showDefaultValues = true)
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin CreateResource createResourceMixin;

  @CommandLine.Option(names = "--project-id", required = true, description = "GCP project id")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "Big Query dataset id")
  private String bigQueryDatasetId;

  @CommandLine.Mixin Format formatOption;

  /** Add a referenced Big Query dataset to the workspace. */
  @Override
  protected void execute() {
    // build the resource object to add
    ResourceDescription resourceToAdd =
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .name(createResourceMixin.name)
                    .description(createResourceMixin.description)
                    .cloningInstructions(createResourceMixin.cloning))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpBqDataset(
                        new GcpBigQueryDatasetAttributes()
                            .projectId(gcpProjectId)
                            .datasetId(bigQueryDatasetId)));

    ResourceDescription resourceAdded =
        new WorkspaceManager(globalContext, workspaceContext)
            .createReferencedBigQueryDataset(resourceToAdd);
    formatOption.printReturnValue(resourceAdded, BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added referenced Big Query dataset.");
    PrintingUtils.printResource(returnValue);
  }
}
