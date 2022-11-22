package bio.terra.cli.command.resource.addref;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ReferencedResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsBucketParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource add-ref aws-bucket" command. */
@CommandLine.Command(
    name = "aws-bucket",
    description = "Add a referenced AWS bucket.",
    showDefaultValues = true)
public class AwsBucket extends BaseCommand {
  @CommandLine.Mixin ReferencedResourceCreation referencedResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(UFAwsBucket returnValue) {
    OUT.println("Successfully added referenced AWS bucket.");
    returnValue.print();
  }

  /** Add a referenced AWS bucket to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    // build the resource object to add
    CreateResourceParams.Builder createResourceParams =
        referencedResourceCreationOptions.populateMetadataFields();
    CreateAwsBucketParams.Builder createParams =
        new CreateAwsBucketParams.Builder().resourceFields(createResourceParams.build());

    bio.terra.cli.businessobject.resource.AwsBucket addedResource =
        bio.terra.cli.businessobject.resource.AwsBucket.addReferenced(createParams.build());
    formatOption.printReturnValue(
        new UFAwsBucket(addedResource), bio.terra.cli.command.resource.addref.AwsBucket::printText);
  }
}
