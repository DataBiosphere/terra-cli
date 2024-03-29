package bio.terra.cli.command.resource.create;

import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceCreation;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSageMakerNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.CloudPlatform;
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
  @CommandLine.Mixin ResourceCreation resourceCreationOption;
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

  /** Add a controlled AWS SageMaker Notebook to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.AWS);

    // build the resource object to create. force the resource to be private
    CreateResourceParams createResourceParams =
        resourceCreationOption
            .populateMetadataFields()
            .stewardshipType(StewardshipType.CONTROLLED)
            .accessScope(AccessScope.PRIVATE_ACCESS)
            .cloningInstructions(CloningInstructionsEnum.NOTHING)
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
