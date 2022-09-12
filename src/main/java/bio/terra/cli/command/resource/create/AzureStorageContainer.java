package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAzureStorageContainerParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAzureStorageContainer;
import bio.terra.workspace.model.StewardshipType;
import java.util.UUID;
import picocli.CommandLine;

/**
 * This class corresponds to the fourth-level "terra resource create azure-storage-container"
 * command.
 */
@CommandLine.Command(
    name = "azure-storage-container",
    description = "Add a controlled Azure storage container.",
    showDefaultValues = true)
public class AzureStorageContainer extends BaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--storage-container-name",
      description = "Name of the Azure storage container.")
  private String storageContainerName;

  @CommandLine.Option(
      names = "--storage-account-id",
      description =
          "The resource ID of the storage account in which the container will be created.")
  private UUID storageAccountId;

  /** Print this command's output in text format. */
  private static void printText(UFAzureStorageContainer returnValue) {
    OUT.println("Successfully added controlled Azure storage container.");
    returnValue.print();
  }

  /** Add a controlled Azure storage container to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateAzureStorageContainerParams.Builder createParams =
        new CreateAzureStorageContainerParams.Builder()
            .resourceFields(createResourceParams.build())
            .storageContainerName(storageContainerName)
            .storageAccountId(storageAccountId);

    bio.terra.cli.businessobject.resource.AzureStorageContainer createdResource =
        bio.terra.cli.businessobject.resource.AzureStorageContainer.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFAzureStorageContainer(createdResource), AzureStorageContainer::printText);
  }
}
