package bio.terra.cli.command.server;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.ServerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(name = "set", description = "Set the Terra server to connect to.")
public class Set extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "server name")
  private String serverName;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    new ServerManager(globalContext).updateServer(serverName);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
