package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSagemakerNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSagemakerNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource create aws-notebook" command. */
@CommandLine.Command(
    name = "sagemaker-notebook",
    description = "Add a controlled AWS Sagemaker Notebook instance.",
    showDefaultValues = true,
    sortOptions = false)
public class AwsSagemakerNotebook extends WsmBaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--instance-name",
      description =
          "Name of the Sagemaker Notebook instance. If not provided, a unique folder name will be generated (https://docs.aws.amazon.com/sagemaker/latest/APIReference/API_CreateNotebookInstance.html#sagemaker-CreateNotebookInstance-request-NotebookInstanceName).")
  private String instanceName;

  @CommandLine.Option(
      names = "--instance-type",
      defaultValue = "ml.t2.medium",
      description =
          "The Compute Engine instance type of this instance (https://docs.aws.amazon.com/sagemaker/latest/dg/notebooks-available-instance-types.html).")
  private String instanceType;

  @CommandLine.Option(
      names = "--region",
      defaultValue = "us-east-1",
      description =
          "The AWS region of the Sagemaker Notebook instance (https://docs.aws.amazon.com/general/latest/gr/sagemaker.html).")
  private String region;

  /** Print this command's output in text format. */
  private static void printText(UFAwsSagemakerNotebook returnValue) {
    OUT.println("Successfully added controlled AWS Sagemaker Notebook.");
    returnValue.print();
  }

  /** Add a controlled AWS Sagemaker Notebook to the workspace. */
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

    CreateAwsSagemakerNotebookParams.Builder createParams =
        new CreateAwsSagemakerNotebookParams.Builder()
            .resourceFields(createResourceParams)
            .instanceName(instanceName)
            .instanceType(instanceType)
            .region(region);

    bio.terra.cli.businessobject.resource.AwsSagemakerNotebook createdResource =
        bio.terra.cli.businessobject.resource.AwsSagemakerNotebook.createControlled(
            createParams.build());
    formatOption.printReturnValue(
        new UFAwsSagemakerNotebook(createdResource), AwsSagemakerNotebook::printText);
  }
}
