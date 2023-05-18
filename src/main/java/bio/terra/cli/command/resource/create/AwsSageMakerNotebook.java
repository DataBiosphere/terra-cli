package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.ControlledResourceCreation;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSageMakerNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsSageMakerNotebook;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/**
 * This class corresponds to the fourth-level "terra resource create sagemaker-notebook" command.
 */
@CommandLine.Command(
    name = "sagemaker-notebook",
    description = "Add a controlled AWS SageMaker Notebook instance.",
    showDefaultValues = true,
    sortOptions = false)
public class AwsSageMakerNotebook extends WsmBaseCommand {
  @CommandLine.Mixin ControlledResourceCreation controlledResourceCreationOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--instance-name",
      description =
          "Name of the SageMaker Notebook instance. If not provided, a unique notebook name will be generated (https://docs.aws.amazon.com/sagemaker/latest/APIReference/API_CreateNotebookInstance.html#sagemaker-CreateNotebookInstance-request-NotebookInstanceName).")
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
          "The AWS region of the SageMaker Notebook instance (https://docs.aws.amazon.com/general/latest/gr/sagemaker.html).")
  private String region;

  /** Print this command's output in text format. */
  private static void printText(UFAwsSageMakerNotebook returnValue) {
    OUT.println("Successfully added controlled AWS SageMaker Notebook.");
    returnValue.print();
  }

  /** Add a controlled AWS SageMaker Notebook to the workspace. */
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

    CreateAwsSageMakerNotebookParams.Builder createParams =
        new CreateAwsSageMakerNotebookParams.Builder()
            .resourceFields(createResourceParams)
            .instanceName(instanceName)
            .instanceType(instanceType)
            .region(region);

    bio.terra.cli.businessobject.resource.AwsSageMakerNotebook.createControlled(
        createParams.build());

    OUT.println("Creating notebook instance. It may take a few minutes before it is available");
  }
}
