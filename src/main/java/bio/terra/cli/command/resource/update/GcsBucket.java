package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledCloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketNewName;
import bio.terra.cli.command.shared.options.GcsBucketStorageClass;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsBucketParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Update a GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin GcsBucketNewName newBucketName;
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin GcsBucketStorageClass storageClassOption;
  @CommandLine.Mixin bio.terra.cli.command.shared.options.GcsBucketLifecycle lifecycleOptions;
  @CommandLine.Mixin ControlledCloningInstructionsForUpdate newCloningInstructionsOption;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a bucket in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !storageClassOption.isDefined()
        && !lifecycleOptions.isDefined()
        && newBucketName.getNewBucketName() == null
        && newCloningInstructionsOption.getCloning() == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcsBucket resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GCS_BUCKET);

    if (resource.getStewardshipType().equals(StewardshipType.REFERENCED)) {
      // some options only apply to controlled resources
      if (storageClassOption.isDefined() || lifecycleOptions.isDefined()) {
        throw new UserActionableException(
            "Storage and lifecycle options can only be updated for controlled resources.");
      }
      UpdateReferencedGcsBucketParams.Builder gcsBucketParams =
          new UpdateReferencedGcsBucketParams.Builder()
              .resourceParams(resourceUpdateOptions.populateMetadataFields().build())
              .bucketName(newBucketName.getNewBucketName());
      resource.updateReferenced(gcsBucketParams.build());
    } else {
      resource.updateControlled(
          new UpdateControlledGcsBucketParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .defaultStorageClass(storageClassOption.storageClass)
              .lifecycle(lifecycleOptions.buildLifecycleObject())
              .cloningInstructions(newCloningInstructionsOption.getCloning())
              .build());
    }
    // re-load the resource so we display all properties with up-to-date values
    resource = Context.requireWorkspace()
        .getResource(resource.getName())
        .castToType(Type.GCS_BUCKET);
    formatOption.printReturnValue(new UFGcsBucket(resource), GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully updated GCS bucket.");
    returnValue.print();
  }
}
