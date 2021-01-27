package bio.terra.cli.command.workspace;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(name = "create", description = "Create a new workspace.")
public class Create implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra workspace create");
    return 0;
  }
}
