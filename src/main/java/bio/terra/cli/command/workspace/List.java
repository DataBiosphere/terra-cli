package bio.terra.cli.command.workspace;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list" command. */
@Command(name = "list", description = "List all workspaces accessible to the current user.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra workspace list");
    return 0;
  }
}
