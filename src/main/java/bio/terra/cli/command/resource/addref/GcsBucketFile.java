package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref gcs-bucket-file" command. */
@CommandLine.Command(
    name = "gcs-bucket-file",
    description = "Add a referenced GCS bucket file.",
    showDefaultValues = true)
public class GcsBucketFile extends BaseCommand {
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private String bucketName;

  @CommandLine.Option(
      names = "--file-name",
      required = true,
      description = "Name of the GCS bucket file.")
  private String bucketFileName;

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
    CreateGcsBucketParams.Builder createParams =
        new CreateGcsBucketParams.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketName);

    bio.terra.cli.businessobject.resource.GcsBucket addedResource =
        bio.terra.cli.businessobject.resource.GcsBucket.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGcsBucket(addedResource), GcsBucketFile::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully added referenced GCS bucket file.");
    returnValue.print();
  }
}
