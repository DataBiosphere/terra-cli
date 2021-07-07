package bio.terra.cli.command.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    deletePromptOption.throwIfConfirmationPromptNegative();
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    resource.delete();
    formatOption.printReturnValue(resource.serializeToCommand(), Delete::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFResource returnValue) {
    OUT.println("Successfully deleted resource.");
    returnValue.print();
  }
}
