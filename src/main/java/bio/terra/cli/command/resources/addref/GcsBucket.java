package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.CreateResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Resource;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a referenced GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin CreateResource createResourceOptions;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Mixin Format formatOption;

  /** Add a referenced GCS bucket to the workspace. */
  @Override
  protected void execute() {
    // build the resource object to add
    bio.terra.cli.context.resources.GcsBucket.GcsBucketBuilder resourceToAdd =
        new bio.terra.cli.context.resources.GcsBucket.GcsBucketBuilder().bucketName(bucketName);
    resourceToAdd.stewardshipType(StewardshipType.REFERENCED);
    createResourceOptions.populateMetadataFields(resourceToAdd);

    Resource resource = resourceToAdd.build().addOrCreate();
    formatOption.printReturnValue(resource, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(Resource returnValue) {
    OUT.println("Successfully added referenced GCS bucket.");
    returnValue.printText();
  }
}
