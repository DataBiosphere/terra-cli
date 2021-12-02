package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketName;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateGcsFileParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsFile;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref gcs-bucket-file" command. */
@CommandLine.Command(
    name = "gcs-file",
    description = "Add a referenced GCS bucket file.",
    showDefaultValues = true)
public class GcsFile extends BaseCommand {
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;
  @CommandLine.Mixin GcsBucketName bucketNameOption;

  @CommandLine.Option(
      names = "--file-path",
      required = true,
      description = "Full path to the file or folder in the specified GCS bucket.")
  private String filePath;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced GCS bucket file to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.REFERENCED);
    CreateGcsFileParams.Builder createParams =
        new CreateGcsFileParams.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketNameOption.getBucketName())
            .filePath(filePath);

    bio.terra.cli.businessobject.resource.GcsFile addedResource =
        bio.terra.cli.businessobject.resource.GcsFile.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGcsFile(addedResource), GcsFile::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsFile returnValue) {
    OUT.println("Successfully added referenced GCS bucket file.");
    returnValue.print();
  }
}
