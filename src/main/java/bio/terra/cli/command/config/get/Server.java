package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFServer;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get server" command. */
@Command(name = "server", description = "Get the Terra server the CLI connects to.")
public class Server extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  public static void printText(UFServer returnValue) {
    OUT.println(returnValue.name);
  }

  /** Return the server property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(new UFServer(Context.getServer()), Server::printText);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
