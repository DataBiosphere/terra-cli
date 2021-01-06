package bio.terra.cli.command;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(
    name = "status",
    description = "Print status and details of the Terra server anad workspace context.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println("terra status");

    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("SAM uri: " + globalContext.getSamUri());

    return 0;
  }
}
