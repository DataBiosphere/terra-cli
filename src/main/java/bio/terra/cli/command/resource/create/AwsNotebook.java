package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create aws-notebook" command. */
@CommandLine.Command(
    name = "aws-notebook",
    description =
        "Add a controlled AWS notebook instance.",
    showDefaultValues = true,
    sortOptions = false)
public class AwsNotebook extends BaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--instance-id",
      description =
          "The unique name to give to the notebook instance. Cannot be changed later. "
              + "The instance name must be 1 to 63 characters long and contain only lowercase "
              + "letters, numeric characters, and dashes. The first character must be a lowercase "
              + "letter and the last character cannot be a dash. If not specified, an "
              + "auto-generated name based on your email address and time will be used.")
  private String instanceId;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description =
          "The AWS location of the instance (https://docs.aws.amazon.com/general/latest/gr/sagemaker.html).")
  private String location;

  @CommandLine.Option(
      names = "--instance-type",
      defaultValue = "ml.t2.medium",
      description = "The Compute Engine instance type of this instance (https://docs.aws.amazon.com/sagemaker/latest/dg/notebooks-available-instance-types.html).")
  private String instanceType;

  // TODO(TERRA-228) add additional parameters

  /** Print this command's output in text format. */
  private static void printText(UFAwsNotebook returnValue) {
    OUT.println("Successfully added controlled AWS Notebook instance.");
    returnValue.print();
  }

  /** Add a controlled AWS Notebook instance to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // build the resource object to create. force the resource to be private
    CreateResourceParams.Builder createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED)
            .accessScope(AccessScope.PRIVATE_ACCESS);
    CreateAwsNotebookParams.Builder createParams =
        new CreateAwsNotebookParams.Builder()
            .resourceFields(createResourceParams.build())
            .instanceId(instanceId)
            .location(location)
            .instanceType(instanceType);

    bio.terra.cli.businessobject.resource.AwsNotebook createdResource =
        bio.terra.cli.businessobject.resource.AwsNotebook.createControlled(createParams.build());
    formatOption.printReturnValue(new UFAwsNotebook(createdResource), AwsNotebook::printText);
  }
}
