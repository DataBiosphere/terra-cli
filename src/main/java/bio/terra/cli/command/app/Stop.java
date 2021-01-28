package bio.terra.cli.command.app;

import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app stop" command. */
@Command(name = "stop", description = "Stop a running application in the Terra workspace.")
public class Stop implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to stop: ${COMPLETION-CANDIDATES}")
  private Enable.SupportedForEnableAndStop appName;

  @Override
  public Integer call() {
    // TODO: should we also delete the image on any call to stop?
    GlobalContext globalContext = GlobalContext.readFromFile();
    appName.getToolHelper().stop(globalContext);
    System.out.println(appName.toString() + " successfully stopped.");
    return 0;
  }
}
