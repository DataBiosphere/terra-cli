package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete-property" command. */
@Command(name = "delete-property", description = "Delete the workspace properties.")
public class DeleteProperty extends WsmBaseCommand {
  @CommandLine.Option(
      names = "--keys",
      required = true,
      split = ",",
      description =
          "Workspace properties. Example: --keys=key1. For multiple property keys, use \",\": --keys=key1,key2")
  public List<String> propertyKeys;

  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace updatedWorkspace = Context.requireWorkspace().deleteProperties(propertyKeys);
    updatedWorkspace.listResources();
    formatOption.printReturnValue(new UFWorkspace(updatedWorkspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace properties successfully deleted.");
    returnValue.print();
  }
}
