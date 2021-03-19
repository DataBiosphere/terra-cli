package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra config get-value image" command. */
@Command(
    name = "image",
    description = "[FOR DEBUG] Get the Docker image used for launching applications.")
public class Image implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println(globalContext.dockerImageId);

    return 0;
  }
}
