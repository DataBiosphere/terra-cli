package bio.terra.cli.command.server;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.service.ServerManager;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server list" command. */
@Command(name = "list", description = "List all available Terra servers.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    java.util.List<ServerSpecification> allPossibleServers = ServerManager.allPossibleServers();

    for (ServerSpecification server : allPossibleServers) {
      String prefix = (globalContext.server.equals(server)) ? " * " : "   ";
      System.out.println(prefix + server.name + ": " + server.description);
    }

    return 0;
  }
}
