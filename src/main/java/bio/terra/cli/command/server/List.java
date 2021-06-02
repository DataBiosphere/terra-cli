package bio.terra.cli.command.server;

import bio.terra.cli.Context;
import bio.terra.cli.Server;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandServer;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server list" command. */
@Command(name = "list", description = "List all available Terra servers.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all Terra environments. */
  @Override
  protected void execute() {
    java.util.List<Server> allPossibleServers = Server.list();
    formatOption.printReturnValue(
        allPossibleServers.stream()
            .map(server -> new CommandServer.Builder(server).build())
            .collect(Collectors.toList()),
        this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<CommandServer> returnValue) {
    for (CommandServer server : returnValue) {
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
