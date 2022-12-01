package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace describe" command. */
@Command(name = "describe", description = "Describe the workspace.", showDefaultValues = true)
public class Describe extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Describe the current workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    formatOption.printReturnValue(new UFWorkspace(Context.requireWorkspace()), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    returnValue.print();
  }
}
