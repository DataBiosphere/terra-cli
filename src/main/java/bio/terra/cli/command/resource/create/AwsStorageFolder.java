package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsStorageFolderParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsStorageFolder;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/**
 * This class corresponds to the fourth-level "terra resource create aws-storage-folder" command.
 */
@CommandLine.Command(
    name = "aws-storage-folder",
    description = "Add a controlled AWS storage folder.",
    showDefaultValues = true)
public class AwsStorageFolder extends WsmBaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-east-1",
      description =
          "The AWS location of the storage folder (https://docs.aws.amazon.com/general/latest/gr/s3.html).")
  private String location;

  /** Print this command's output in text format. */
  private static void printText(UFAwsStorageFolder returnValue) {
    OUT.println("Successfully added controlled AWS storage folder.");
    returnValue.print();
  }

  /** Add a controlled AWS storage folder to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateAwsStorageFolderParams.Builder createParams =
        new CreateAwsStorageFolderParams.Builder()
            .resourceFields(createResourceParams.build())
            .region(location);

    bio.terra.cli.businessobject.resource.AwsStorageFolder createdResource =
        bio.terra.cli.businessobject.resource.AwsStorageFolder.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFAwsStorageFolder(createdResource),
        bio.terra.cli.command.resource.create.AwsStorageFolder::printText);
  }
}
