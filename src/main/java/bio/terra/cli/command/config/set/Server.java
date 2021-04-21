package bio.terra.cli.command.config.set;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.ServerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(
    name = "server",
    description =
        "Set the Terra server to connect to. Run `terra server list` to see the available servers.")
public class Server extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "server name")
  private String serverName;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    String prevServerName = globalContext.server.name;
    new ServerManager(globalContext).updateServer(serverName);

    OUT.println(
        "Terra server is set to "
            + globalContext.server.name
            + " ("
            + (globalContext.server.name.equals(prevServerName) ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
