package bio.terra.cli.command.resources.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GcsBucketStorageClass;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.inputs.UpdateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources update gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Update a GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin GcsBucketStorageClass storageClassOption;
  @CommandLine.Mixin bio.terra.cli.command.shared.options.GcsBucketLifecycle lifecycleOptions;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a Big Query dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !storageClassOption.isDefined()
        && !lifecycleOptions.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resources.GcsBucket resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GCS_BUCKET);

    if (resource.getStewardshipType().equals(StewardshipType.REFERENCED)) {
      // some options only apply to controlled resources
      if (storageClassOption.isDefined() || lifecycleOptions.isDefined()) {
        throw new UserActionableException(
            "Storage and lifecycle options can only be updated for controlled resources.");
      }
      resource.updateReferenced(resourceUpdateOptions.populateMetadataFields().build());
    } else {
      resource.updateControlled(
          new UpdateGcsBucketParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .defaultStorageClass(storageClassOption.storageClass)
              .lifecycle(lifecycleOptions.buildLifecycleObject())
              .build());
    }
    formatOption.printReturnValue(new UFGcsBucket(resource), GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucket returnValue) {
    OUT.println("Successfully updated GCS bucket.");
    returnValue.print();
  }
}
