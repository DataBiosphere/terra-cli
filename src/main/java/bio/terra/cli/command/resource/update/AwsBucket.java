package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.AwsBucketLifecycle;
import bio.terra.cli.command.shared.options.AwsBucketNewName;
import bio.terra.cli.command.shared.options.AwsBucketStorageClass;
import bio.terra.cli.command.shared.options.CloningInstructionsForUpdate;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledAwsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedAwsBucketParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.workspace.model.StewardshipType;
import com.google.api.client.util.Strings;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update aws-bucket" command. */
@CommandLine.Command(
    name = "aws-bucket",
    description = "Update a AWS bucket.",
    showDefaultValues = true)
public class AwsBucket extends BaseCommand {
  @CommandLine.Mixin AwsBucketNewName newBucketName;
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin AwsBucketStorageClass storageClassOption;
  @CommandLine.Mixin AwsBucketLifecycle lifecycleOptions;
  @CommandLine.Mixin CloningInstructionsForUpdate newCloningInstructionsOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(UFAwsBucket returnValue) {
    OUT.println("Successfully updated AWS bucket.");
    returnValue.print();
  }

  /** Update a bucket in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && !storageClassOption.isDefined()
        && !lifecycleOptions.isDefined()
        && Strings.isNullOrEmpty(newBucketName.getNewBucketName())
        && newCloningInstructionsOption.getCloning() == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.AwsBucket resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Type.AWS_BUCKET);

    if (resource.getStewardshipType().equals(StewardshipType.REFERENCED)) {
      // some options only apply to controlled resources
      if (storageClassOption.isDefined() || lifecycleOptions.isDefined()) {
        throw new UserActionableException(
            "Storage and lifecycle options can only be updated for controlled resources.");
      }
      UpdateReferencedAwsBucketParams.Builder awsBucketParams =
          new UpdateReferencedAwsBucketParams.Builder()
              .resourceParams(resourceUpdateOptions.populateMetadataFields().build())
              .bucketName(newBucketName.getNewBucketName())
              .cloningInstructions(newCloningInstructionsOption.getCloning());
      resource.updateReferenced(awsBucketParams.build());
    } else {
      resource.updateControlled(
          new UpdateControlledAwsBucketParams.Builder()
              .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
              .defaultStorageClass(storageClassOption.storageClass)
              .lifecycle(lifecycleOptions.buildLifecycleObject())
              .cloningInstructions(newCloningInstructionsOption.getCloning())
              .build());
    }
    // re-load the resource so we display all properties with up-to-date values
    resource =
        Context.requireWorkspace().getResource(resource.getName()).castToType(Type.AWS_BUCKET);
    formatOption.printReturnValue(new UFAwsBucket(resource), AwsBucket::printText);
  }
}
