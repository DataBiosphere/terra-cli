package bio.terra.cli.command.app;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app enable" command. */
@Command(name = "enable", description = "Enable an application in the Terra workspace.")
public class Enable implements Callable<Integer> {

  @Override
  public Integer call() {
    //    GlobalContext globalContext = GlobalContext.readFromFile();
    System.out.println("terra app enable");
    // take argument enum for supported commands and do any command-specific setup (nextflow, gcloud
    // for now)
    // pull the Docker image
    return 0;
  }
}
