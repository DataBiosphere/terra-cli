package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace set-property" command. */
// This is set-property instead of add-property because this can be used to 1) Add property 2)
// Update existing property.
@Command(name = "set-property", description = "Set the workspace properties.")
public class SetProperty extends BaseCommand {
  @CommandLine.Mixin Format formatOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(
      names = "--properties",
      required = true,
      split = ",",
      description =
          "Workspace properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> workspaceProperties;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace updatedWorkspace = Context.requireWorkspace().updateProperties(workspaceProperties);
    updatedWorkspace.listResourcesAndSync();
    formatOption.printReturnValue(new UFWorkspace(updatedWorkspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace properties successfully updated.");
    returnValue.print();
  }
}
