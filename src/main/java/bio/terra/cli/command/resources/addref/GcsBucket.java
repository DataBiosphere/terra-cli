package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CreateResource;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateGcsBucket;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateResource;
import bio.terra.cli.serialization.command.resources.CommandGcsBucket;
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
    CreateUpdateResource.Builder createResourceParams =
        createResourceOptions.populateMetadataFields().stewardshipType(StewardshipType.REFERENCED);
    CreateUpdateGcsBucket.Builder createParams =
        new CreateUpdateGcsBucket.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketName);

    bio.terra.cli.businessobject.resources.GcsBucket addedResource =
        bio.terra.cli.businessobject.resources.GcsBucket.addReferenced(createParams.build());
    formatOption.printReturnValue(new CommandGcsBucket(addedResource), GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CommandGcsBucket returnValue) {
    OUT.println("Successfully added referenced GCS bucket.");
    returnValue.print();
  }
}
