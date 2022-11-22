package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get workspace" command. */
@Command(name = "workspace", description = "Get the current Terra workspace.")
public class Workspace extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  public static void printText(UFWorkspace returnValue) {
    OUT.println(returnValue == null ? "" : returnValue.id);
  }

  /** Return the workspace property of the global context. */
  @Override
  protected void execute() {
    UFWorkspace currentWorkspace =
        Context.getWorkspace().isPresent() ? new UFWorkspace(Context.requireWorkspace()) : null;
    formatOption.printReturnValue(currentWorkspace, Workspace::printText);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
