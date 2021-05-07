package bio.terra.cli.command.server;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.service.ServerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server list" command. */
@Command(name = "list", description = "List all available Terra servers.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all Terra environments. */
  @Override
  protected void execute() {
    java.util.List<ServerSpecification> allPossibleServers = ServerManager.allPossibleServers();
    formatOption.printReturnValue(allPossibleServers, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<ServerSpecification> returnValue) {
    for (ServerSpecification server : returnValue) {
      String prefix = (globalContext.server.equals(server)) ? " * " : "   ";
      OUT.println(prefix + server.name + ": " + server.description);
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
