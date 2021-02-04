package bio.terra.cli.command.app;

import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app set-image" command. */
@Command(name = "get-image", description = "Get the Docker image used for launching applications.")
public class GetImage implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println("Docker image used is " + globalContext.dockerImageId + ".");

    return 0;
  }
}
