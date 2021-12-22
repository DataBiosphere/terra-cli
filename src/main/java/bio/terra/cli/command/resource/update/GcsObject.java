package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketNewName;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update gcs-object" command. */
@CommandLine.Command(
    name = "gcs-object",
    description = "Update a GCS bucket object.",
    showDefaultValues = true)
public class GcsObject extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin GcsBucketNewName newBucketName;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--new-object-name",
      description = "Full path to the object in the specified GCS bucket.")
  private String newObjectName;

  /** Update a bucket object in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && newObjectName == null
        && newBucketName.getNewBucketName() == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcsObject resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GCS_OBJECT);

    UpdateResourceParams updateResourceParams =
        resourceUpdateOptions.populateMetadataFields().build();
    UpdateReferencedGcsObjectParams gcsObjectParams =
        new UpdateReferencedGcsObjectParams.Builder()
            .resourceFields(updateResourceParams)
            .bucketName(newBucketName.getNewBucketName())
            .objectName(newObjectName)
            .originalObjectName(resource.getObjectName())
            .originalBucketName(resource.getBucketName())
            .build();
    resource.updateReferenced(gcsObjectParams);

    formatOption.printReturnValue(new UFGcsObject(resource), GcsObject::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsObject returnValue) {
    OUT.println("Successfully updated GCS bucket object.");
    returnValue.print();
  }
}
