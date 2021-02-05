package bio.terra.cli.command.app;

import bio.terra.cli.apps.interfaces.Supported;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app list" command. */
@Command(name = "list", description = "List the supported applications.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println(
        "Call any of the supported applications listed below, by prefixing it with 'terra' (e.g. terra gsutil ls, terra nextflow run hello)\n");
    for (Supported.SupportedApp app : Supported.SupportedApp.values()) {
      System.out.println("  " + app);
    }

    return 0;
  }
}
