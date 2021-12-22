package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketName;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.AddGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref gcs-object" command. */
@CommandLine.Command(
    name = "gcs-object",
    description = "Add a referenced GCS bucket object.",
    showDefaultValues = true)
public class GcsObject extends BaseCommand {
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;
  @CommandLine.Mixin GcsBucketName bucketNameOption;

  @CommandLine.Option(
      names = "--object-name",
      required = true,
      description =
          "Full path to the object in the specified GCS bucket, such as folder1/file.txt and folder1/")
  private String objectName;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a referenced GCS bucket object to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        resourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.REFERENCED);
    AddGcsObjectParams.Builder createParams =
        new AddGcsObjectParams.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketNameOption.getBucketName())
            .objectName(objectName);

    bio.terra.cli.businessobject.resource.GcsObject addedResource =
        bio.terra.cli.businessobject.resource.GcsObject.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGcsObject(addedResource), GcsObject::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsObject returnValue) {
    OUT.println("Successfully added referenced GCS bucket object.");
    returnValue.print();
  }
}
