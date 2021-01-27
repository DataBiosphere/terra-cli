package bio.terra.cli.command.app;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app stop" command. */
@Command(name = "stop", description = "Stop a running application in the Terra workspace.")
public class Stop implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra app stop");
    // take argument enum for supported commands and do any command-specific cleanup (nextflow,
    // gcloud for now)
    // delete the docker image?
    return 0;
  }
}
