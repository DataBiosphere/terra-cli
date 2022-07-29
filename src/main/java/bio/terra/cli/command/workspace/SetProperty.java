package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.command.shared.options.WorkspaceProperties;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace set-property" command. */
@Command(name = "set-property", description = "Set the workspace properties.")
public class SetProperty extends BaseCommand {
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin WorkspaceProperties workspaceProperties;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace updatedWorkspace =
        Context.requireWorkspace().updateProperties(workspaceProperties.properties);
    updatedWorkspace.listResourcesAndSync();
    formatOption.printReturnValue(new UFWorkspace(updatedWorkspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace properties successfully updated.");
    returnValue.print();
  }
}
