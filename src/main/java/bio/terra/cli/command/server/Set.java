package bio.terra.cli.command.server;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.service.ServerManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(name = "set", description = "Set the Terra server to connect to.")
public class Set implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "server name")
  private String serverName;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    ServerSpecification prevServer = globalContext.server;
    new ServerManager(globalContext).updateServer(serverName);

    if (globalContext.server.equals(prevServer)) {
      System.out.println("Terra server: " + globalContext.server.name + " (UNCHANGED)");
    } else {
      System.out.println(
          "Terra server: " + globalContext.server.name + " (CHANGED FROM " + prevServer.name + ")");
    }

    return 0;
  }
}
