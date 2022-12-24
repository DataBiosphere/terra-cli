package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
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
    header = "Add a controlled GCS bucket.",
    description =
        "Adds a controlled GCS bucket resource. \n\n"
            + "== Lifecycle rules \n\n"
            + "GCS bucket lifecycle rules are specified by passing a JSON-formatted file path to the `terra resource create gcs-bucket` command. \n\n"
            + "The expected JSON structure matches the one used by the https://cloud.google.com/storage/docs/gsutil/commands/lifecycle[gsutil lifecycle command]. "
            + "This structure is a subset of the https://cloud.google.com/storage/docs/json_api/v1/buckets#lifecycle[GCS resource specification]. \n\n"
            + "There is a command shortcut for specifying a lifecycle rule which deletes any objects more than N days old: \n\n"
            + ".... \n\n"
            + "terra resource create gcs-bucket --name=mybucket --bucket-name=mybucket --auto-delete=365 \n\n"
            + ".... \n\n",
    showDefaultValues = true)
public class GcsBucket extends WsmBaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin GcsBucketStorageClass storageClassOption;
  @CommandLine.Mixin bio.terra.cli.command.shared.options.GcsBucketLifecycle lifecycleOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--bucket-name",
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket'). If not provided, a unique bucket name will be generated.")
  private String bucketName;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "US-CENTRAL1",
      description =
          "The Google Cloud location of the bucket (https://cloud.google.com/storage/docs/locations).")
  private String location;

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    returnValue.print();
  }

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
}
