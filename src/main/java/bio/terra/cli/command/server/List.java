package bio.terra.cli.command.server;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFServer;
import bio.terra.cli.utils.Printer;
import java.util.Comparator;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server list" command. */
@Command(name = "list", description = "List all available Terra servers.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all Terra environments. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        Printer.sortAndMap(Server.list(), Comparator.comparing(Server::getName), UFServer::new),
        this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<UFServer> returnValue) {
    for (UFServer server : returnValue) {
      String prefix = (Context.getServer().getName().equals(server.name)) ? " * " : "   ";
      OUT.println(prefix + server.name + ": " + server.description);
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
