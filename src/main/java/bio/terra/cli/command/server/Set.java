package bio.terra.cli.command.server;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.ConfirmationPrompt;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(name = "set", description = "Set the Terra server to connect to.")
public class Set extends BaseCommand {
  @CommandLine.Mixin ConfirmationPrompt confirmationPromptOption;

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Server name. Run `terra server list` to see the available servers.")
  private String name;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    String prevServerName = Context.getServer().getName();
    Server newServer = Server.get(name);

    boolean serverChanged = !prevServerName.equals(newServer.getName());
    boolean loggedIn = Context.getUser().isPresent();
    boolean workspaceSet = Context.getWorkspace().isPresent();
    if (serverChanged && (loggedIn || workspaceSet)) {
      confirmationPromptOption.confirmOrThrow(
          "Switching the server will clear the current login credentials and workspace. Are you sure you want to proceed (y/N)?",
          "Switching server aborted.");
    }

    // unset the current user and workspace
    if (loggedIn) {
      Context.requireUser().logout();
    }
    if (workspaceSet) {
      Context.setWorkspace(null);
    }
    Context.setServer(newServer);

    OUT.println(
        "Terra server is set to "
            + Context.getServer().getName()
            + " ("
            + (serverChanged ? "CHANGED" : "UNCHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
