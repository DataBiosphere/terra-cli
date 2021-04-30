package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateControlledResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.PrivateResourceIamRoles;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
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

    // build the resource object to create
    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (createControlledResourceMixin.privateIamRoles != null
        && !createControlledResourceMixin.privateIamRoles.isEmpty()) {
      privateResourceIamRoles.addAll(createControlledResourceMixin.privateIamRoles);
    }
    ResourceDescription resourceToCreate =
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .name(createControlledResourceMixin.name)
                    .description(createControlledResourceMixin.description)
                    .cloningInstructions(createControlledResourceMixin.cloning)
                    .controlledResourceMetadata(
                        new ControlledResourceMetadata()
                            .accessScope(createControlledResourceMixin.access)
                            .privateResourceUser(
                                new PrivateResourceUser()
                                    .userName(createControlledResourceMixin.privateUserEmail)
                                    .privateResourceIamRoles(privateResourceIamRoles))))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpBqDataset(new GcpBigQueryDatasetAttributes().datasetId(bigQueryDatasetId)));

    ResourceDescription resourceCreated =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledBigQueryDataset(resourceToCreate, location);
    formatOption.printReturnValue(resourceCreated, BqDataset::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled Big Query dataset.");
    PrintingUtils.printResource(returnValue);
  }
}
