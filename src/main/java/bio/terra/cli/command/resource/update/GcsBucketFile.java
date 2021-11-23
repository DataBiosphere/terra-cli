package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucketFile;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update gcs-bucket-file" command. */
@CommandLine.Command(
    name = "gcs-bucket-file",
    description = "Update a GCS bucket file.",
    showDefaultValues = true)
public class GcsBucketFile extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a BigQuery bucket file in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcsBucketFile resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.GCS_BUCKET_FILE);

    resource.updateReferenced(resourceUpdateOptions.populateMetadataFields().build());

    formatOption.printReturnValue(new UFGcsBucketFile(resource), GcsBucketFile::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGcsBucketFile returnValue) {
    OUT.println("Successfully updated GCS bucket file.");
    returnValue.print();
  }
}
