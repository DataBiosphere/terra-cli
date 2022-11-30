package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update gcs-object" command. */
@CommandLine.Command(
    name = "gcs-object",
    description = "Update a GCS bucket object.",
    showDefaultValues = true)
public class GcsObject extends BaseCommand {
  @CommandLine.Mixin CloningInstructionsForUpdate newCloningInstructionsOption;
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--new-bucket-name",
      description =
          "New name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private String newBucketName;

  @CommandLine.Option(
      names = "--new-object-name",
      description = "Full path to the object in the specified GCS bucket.")
  private String newObjectName;

  @CommandLine.Option(
      names = "--new-path",
      description = "New path of the bucket (e.g. 'gs://bucket_name/object/path').")
  public String newPath;

  /** Print this command's output in text format. */
  private static void printText(UFGcsObject returnValue) {
    OUT.println("Successfully updated GCS bucket object.");
    returnValue.print();
  }

  /** Update a bucket object in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && newObjectName == null
        && newBucketName == null
        && newPath == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // parsing the path as bucket name and object name
    if (newPath != null) {
      if (newBucketName != null || newObjectName != null) {
        throw new UserActionableException(
            "No need to specify --new-bucket-name and/or --new-object-name when --new-path is specified.");
      }
      Pattern r = Pattern.compile("(?:^gs://)([^/]*)/(.*)");
      Matcher m = r.matcher(newPath);
      if (!m.find()) {
        throw new UserActionableException(
            "Specify a legal path, like 'gs://bucket_name/object/path'.");
      }
      // the first group is "gs://" after regular expression parsing
      newBucketName = m.group(1);
      newObjectName = m.group(2);
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcsObject resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GCS_OBJECT);

    UpdateReferencedGcsObjectParams gcsObjectParams =
        new UpdateReferencedGcsObjectParams.Builder()
            .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
            .bucketName(newBucketName)
            .objectName(newObjectName)
            .cloningInstructions(newCloningInstructionsOption.getCloning())
            .build();
    resource.updateReferenced(gcsObjectParams);

    // re-load the resource so we display all properties with up-to-date values
    resource =
        Context.requireWorkspace().getResource(resource.getName()).castToType(Type.GCS_OBJECT);
    formatOption.printReturnValue(new UFGcsObject(resource), GcsObject::printText);
  }
}
