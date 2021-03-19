package bio.terra.cli.command.config;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println("browser launch for login = " + globalContext.browserLaunchOption);
    System.out.println("docker image id = " + globalContext.dockerImageId);

    return 0;
  }
}
