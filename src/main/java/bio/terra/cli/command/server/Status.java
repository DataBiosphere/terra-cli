package bio.terra.cli.command.server;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.service.ServerManager;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server status" command. */
@Command(name = "status", description = "Print status and details of the Terra server context.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println(
        "Current server: "
            + globalContext.server.name
            + " ("
            + globalContext.server.description
            + ")");

    boolean serverIsOk = new ServerManager(globalContext).pingServerStatus();
    System.out.println("Server status: " + (serverIsOk ? "OKAY" : "ERROR CONNECTING"));

    return 0;
  }
}
