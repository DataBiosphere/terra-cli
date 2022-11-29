package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledAwsNotebookParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsNotebook;
import picocli.CommandLine;

@CommandLine.Command(
    name = "aws-notebook",
    description = "Update the AWS notebook.",
    showDefaultValues = true)
public class AwsNotebook extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(UFAwsNotebook returnValue) {
    OUT.println("Successfully updated AWS notebook.");
    returnValue.print();
  }

  /** Update a AWS notebook in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    bio.terra.cli.businessobject.resource.AwsNotebook resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Type.AWS_SAGEMAKER_NOTEBOOK);
    resource.updateControlled(
        new UpdateControlledAwsNotebookParams.Builder()
            .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
            .build());

    // re-load the resource so we display all properties with up-to-date values
    resource = Context.requireWorkspace().getResource(resource.getName()).castToType(Type.AWS_SAGEMAKER_NOTEBOOK);
    formatOption.printReturnValue(new UFAwsNotebook(resource), AwsNotebook::printText);
  }
}
