package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketStorageClass;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a controlled GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;

  @CommandLine.Option(
      names = "--bucket-name",
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private String bucketName;

  @CommandLine.Mixin GcsBucketStorageClass storageClassOption;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "US-CENTRAL1",
      description = "Bucket location (https://cloud.google.com/storage/docs/locations).")
  private String location;

  @CommandLine.Mixin bio.terra.cli.command.shared.options.GcsBucketLifecycle lifecycleOptions;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a controlled GCS bucket to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateGcsBucketParams.Builder createParams =
        new CreateGcsBucketParams.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketName)
            .defaultStorageClass(storageClassOption.storageClass)
            .location(location)
            .lifecycle(lifecycleOptions.buildLifecycleObject());

    bio.terra.cli.businessobject.resource.GcsBucket createdResource =
        bio.terra.cli.businessobject.resource.GcsBucket.createControlled(createParams.build());
    formatOption.printReturnValue(new UFGcsBucket(createdResource), GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    returnValue.print();
  }
}
