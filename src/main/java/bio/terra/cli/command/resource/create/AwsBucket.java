package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsBucketParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create aws-bucket" command. */
@CommandLine.Command(
    name = "aws-bucket",
    description = "Add a controlled AWS bucket.",
    showDefaultValues = true)
public class AwsBucket extends BaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-east-1",
      description = "Bucket location (https://docs.aws.amazon.com/general/latest/gr/s3.html).")
  private String location;

  /** Print this command's output in text format. */
  private static void printText(UFAwsBucket returnValue) {
    OUT.println("Successfully added controlled AWS bucket.");
    returnValue.print();
  }

  /** Add a controlled AWS bucket to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED);

    CreateAwsBucketParams.Builder createParams =
        new CreateAwsBucketParams.Builder()
            .resourceFields(createResourceParams.build())
            .location(location);

    bio.terra.cli.businessobject.resource.AwsBucket createdResource = bio.terra.cli.businessobject.resource.AwsBucket.createControlled(createParams.build());
    formatOption.printReturnValue(new UFAwsBucket(createdResource), bio.terra.cli.command.resource.create.AwsBucket::printText);
  }
}
