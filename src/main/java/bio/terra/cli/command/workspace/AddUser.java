package bio.terra.cli.command.workspace;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user to the workspace.")
public class AddUser implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra workspace add-user");
    return 0;
  }
}
