package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref gcs-object" command. */
@CommandLine.Command(
    name = "gcs-object",
    description = "Add a referenced GCS bucket object.",
    showDefaultValues = true)
public class GcsObject extends BaseCommand {
  @CommandLine.Mixin ReferencedResourceCreation referencedResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--bucket-name",
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private String bucketName;

  @CommandLine.Option(
      names = "--object-name",
      description =
          "Full path to the object in the specified GCS bucket, such as folder1/file.txt and folder1/")
  private String objectName;

  @CommandLine.Option(
      names = "--path",
      description = "Path of the bucket (e.g. 'gs://bucket_name/object/path').")
  public String gcsPath;

  /** Print this command's output in text format. */
  private static void printText(UFGcsObject returnValue) {
    OUT.println("Successfully added referenced GCS bucket object.");
    returnValue.print();
  }

  /** Add a referenced GCS bucket object to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        referencedResourceCreationOptions.populateMetadataFields();

    // parsing the path as bucket name and object name
    if (gcsPath != null) {
      if (bucketName != null || objectName != null) {
        throw new UserActionableException(
            "Specify either --path or both --bucket-name and --object-name.");
      }
      Pattern r = Pattern.compile("(?:^gs://)([^/]*)/(.*)");
      Matcher m = r.matcher(gcsPath);
      if (!m.find()) {
        throw new UserActionableException(
            "Specify a legal path, like 'gs://bucket_name/object/path'.");
      }
      // the first group is "gs://" after regular expression parsing
      bucketName = m.group(1);
      objectName = m.group(2);
    } else {
      if (bucketName == null || objectName == null) {
        throw new UserActionableException("Specify at least one path to add.");
      }
    }

    AddGcsObjectParams.Builder createParams =
        new AddGcsObjectParams.Builder()
            .resourceFields(createResourceParams.build())
            .bucketName(bucketName)
            .objectName(objectName);

    bio.terra.cli.businessobject.resource.GcsObject addedResource =
        bio.terra.cli.businessobject.resource.GcsObject.addReferenced(createParams.build());
    formatOption.printReturnValue(new UFGcsObject(addedResource), GcsObject::printText);
  }
}
