package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace enable-pet" command. */
@Command(
    name = "enable-pet",
    description = "Allow the caller to impersonate their pet in this workspace.",
    showDefaultValues = true)
public class EnablePet extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Allow a user to actAs their pet in the current workspace context. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    String petEmail = Workspace.enablePet();
    formatOption.printReturnValue(petEmail);
  }
}
