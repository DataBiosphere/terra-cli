package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsS3StorageFolderParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create s3-storage-folder" command. */
@CommandLine.Command(
    name = "s3-storage-folder",
    description = "Add a controlled AWS S3 Storage Folder.",
    showDefaultValues = true)
public class AwsS3StorageFolder extends WsmBaseCommand {
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
  private static void printText(UFAwsS3StorageFolder returnValue) {
    OUT.println("Successfully added controlled AWS S3 Storage Folder.");
    returnValue.print();
  }

  /** Add a controlled AWS S3 Storage Folder to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateAwsS3StorageFolderParams.Builder createParams =
        new CreateAwsS3StorageFolderParams.Builder()
            .resourceFields(createResourceParams.build())
            .region(location);

    bio.terra.cli.businessobject.resource.AwsS3StorageFolder createdResource =
        bio.terra.cli.businessobject.resource.AwsS3StorageFolder.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFAwsS3StorageFolder(createdResource), AwsS3StorageFolder::printText);
  }
}
