package bio.terra.cli.command;

import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(
    name = "status",
    description = "Print status and details of the Terra server anad workspace context.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println(
        "Current server is " + globalContext.server.name + ": " + globalContext.server.description);

    boolean serverIsOk = globalContext.server.pingServerStatus();
    System.out.println("Current server status: " + (serverIsOk ? "OKAY" : "ERROR CONNECTING"));

    return 0;
  }
}
