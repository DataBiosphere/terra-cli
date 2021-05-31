package bio.terra.cli.command.server;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.Server;
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
    String prevServerName = globalContext.getServer().name;
    Server.switchTo(name);

    OUT.println(
        "Terra server is set to "
            + globalContext.getServer().name
            + " ("
            + (globalContext.getServer().name.equals(prevServerName) ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
