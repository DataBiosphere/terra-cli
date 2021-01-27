package bio.terra.cli.command.workspace;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete this workspace.")
public class Delete implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra workspace delete");
    return 0;
  }
}
