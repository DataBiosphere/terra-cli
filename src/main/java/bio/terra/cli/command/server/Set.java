package bio.terra.cli.command.server;

import bio.terra.cli.Context;
import bio.terra.cli.Server;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(name = "set", description = "Set the Terra server to connect to.")
public class Set extends BaseCommand {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Server name. Run `terra server list` to see the available servers.")
  private String name;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    String prevServerName = Context.getServer().getName();
    Context.setServer(Server.get(name));

    OUT.println(
        "Terra server is set to "
            + Context.getServer().getName()
            + " ("
            + (Context.getServer().getName().equals(prevServerName) ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
