package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace set" command. */
@Command(name = "set", description = "Set the workspace to an existing one.")
public class Set extends BaseCommand {

  @CommandLine.Option(names = "--id", required = true, description = "workspace id")
  private UUID id;

  @CommandLine.Option(
      names = "--defer-login",
      hidden = true,
      description = "Defer login and skip fetching the workspace metadata.")
  private boolean deferLogin;

  @CommandLine.Mixin Format formatOption;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspace = Workspace.load(id);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully loaded.");
    returnValue.print();
  }

  /**
   * Typical usage (--defer-login not specified) requires login because we use the user's
   * credentials to fetch the workspace metadata from WSM. If the --defer-login flag is specified
   * and the user is not already logged in, then we skip the login prompt and fetch the metadata the
   * next time the user logs in.
   */
  protected boolean requiresLogin() {
    return !deferLogin;
  }
}
