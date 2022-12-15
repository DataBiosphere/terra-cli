package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
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
    description = "Add a controlled AWS notebook instance.",
    showDefaultValues = true,
    sortOptions = false)
public class AwsNotebook extends WsmBaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-east-1",
      description =
          "The AWS location of the instance (https://docs.aws.amazon.com/general/latest/gr/sagemaker.html).")
  private String location;

  @CommandLine.Option(
      names = "--instance-type",
      defaultValue = "ml.t2.medium",
      description =
          "The Compute Engine instance type of this instance (https://docs.aws.amazon.com/sagemaker/latest/dg/notebooks-available-instance-types.html).")
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
    CreateResourceParams createResourceParams =
        controlledResourceCreationOptions
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED)
            .accessScope(AccessScope.PRIVATE_ACCESS)
            .build();

    CreateAwsNotebookParams.Builder createParams =
        new CreateAwsNotebookParams.Builder()
            .resourceFields(createResourceParams)
            .instanceId(createResourceParams.name)
            .location(location)
            .instanceType(instanceType);

    bio.terra.cli.businessobject.resource.AwsNotebook createdResource =
        bio.terra.cli.businessobject.resource.AwsNotebook.createControlled(createParams.build());
    formatOption.printReturnValue(new UFAwsNotebook(createdResource), AwsNotebook::printText);
  }
}
