package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsS3StorageFolderParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
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
      names = "--folder-name",
      description =
          "Name of the S3 Storage Folder, without the prefix. (e.g. 'my-folder', not 's3://my-folder'). If not provided, a unique folder name will be generated.")
  private String folderName;

  @CommandLine.Option(
      names = "--region",
      defaultValue = "us-east-1",
      description =
          "The AWS region of the S3 Storage Folder (https://docs.aws.amazon.com/general/latest/gr/s3.html).")
  private String region;

  /** Print this command's output in text format. */
  private static void printText(UFAwsS3StorageFolder returnValue) {
    OUT.println("Successfully added controlled AWS S3 Storage Folder.");
    returnValue.print();
  }

  /** Add a controlled AWS S3 Storage Folder to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.AWS);

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateAwsS3StorageFolderParams.Builder createParams =
        new CreateAwsS3StorageFolderParams.Builder()
            .resourceFields(createResourceParams.build())
            .folderName(folderName)
            .region(region);

    bio.terra.cli.businessobject.resource.AwsS3StorageFolder createdResource =
        bio.terra.cli.businessobject.resource.AwsS3StorageFolder.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFAwsS3StorageFolder(createdResource), AwsS3StorageFolder::printText);
  }
}
