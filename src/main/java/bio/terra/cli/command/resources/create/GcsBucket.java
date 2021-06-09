package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CreateControlledResource;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateGcsBucket;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.cli.serialization.userfacing.inputs.GcsBucketLifecycle;
import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.databind.MapperFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a controlled GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin CreateControlledResource createControlledResourceOptions;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Option(
      names = "--storage",
      description =
          "Storage class (https://cloud.google.com/storage/docs/storage-classes): ${COMPLETION-CANDIDATES}")
  private GcpGcsBucketDefaultStorageClass storageClass;

  @CommandLine.Option(
      names = "--location",
      description = "Bucket location (https://cloud.google.com/storage/docs/locations)")
  private String location;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  GcsBucket.LifecycleArgGroup lifecycleArgGroup;

  static class LifecycleArgGroup {
    @CommandLine.Option(
        names = "--lifecycle",
        description =
            "Lifecycle rules (https://cloud.google.com/storage/docs/lifecycle) specified in a JSON-formatted file. See the README for the expected JSON format.")
    private Path pathToLifecycleFile;

    @CommandLine.Option(
        names = "--auto-delete",
        description =
            "Number of days after which to auto-delete the objects in the bucket. This option is a shortcut for specifying a lifecycle rule that auto-deletes objects in the bucket after some number of days.")
    private Integer autoDelete;
  }

  @CommandLine.Mixin Format formatOption;

  /** Add a controlled GCS bucket to the workspace. */
  @Override
  protected void execute() {
    createControlledResourceOptions.validateAccessOptions();

    // build the resource object to create
    CreateUpdateResource.Builder createResourceParams =
        createControlledResourceOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);
    CreateUpdateGcsBucket.Builder createParams =
        new CreateUpdateGcsBucket.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketName)
            .defaultStorageClass(storageClass)
            .location(location);

    // build the lifecycle object
    if (lifecycleArgGroup == null) {
      // empty lifecycle rule object
      createParams.lifecycle(new GcsBucketLifecycle());
    } else if (lifecycleArgGroup.autoDelete != null) {
      // build an auto-delete lifecycle rule and set the number of days
      createParams.lifecycle(GcsBucketLifecycle.buildAutoDeleteRule(lifecycleArgGroup.autoDelete));
    } else {
      // read in the lifecycle rules from a file
      try {
        createParams.lifecycle(
            JacksonMapper.readFileIntoJavaObject(
                lifecycleArgGroup.pathToLifecycleFile.toFile(),
                GcsBucketLifecycle.class,
                Collections.singletonList(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)));
      } catch (IOException ioEx) {
        throw new UserActionableException("Error reading lifecycle rules from file.", ioEx);
      }
    }

    bio.terra.cli.businessobject.resources.GcsBucket createdResource =
        bio.terra.cli.businessobject.resources.GcsBucket.createControlled(createParams.build());
    formatOption.printReturnValue(new UFGcsBucket(createdResource), GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    returnValue.print();
  }
}
