package bio.terra.cli.command.app;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app list" command. */
@Command(name = "list", description = "List the supported applications.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println(
        "Call any of the supported applications listed below, by prefixing it with 'terra' (e.g. terra gsutil ls, terra nextflow run hello)\n");
    for (PassThrough app : PassThrough.values()) {
      System.out.println("  " + app);
    }

    return 0;
  }

  /**
   * This enum specifies the list returned by the 'terra app list' command. These commands are
   * hidden top-level commands that can be called in a pass-through manner, meaning the user can
   * call them as if they were running the commands locally, except with a "terra" prefix. This
   * prefix means that the command will run within the context of a Terra workspace (e.g. in the
   * backing Google project, wtih data references scoped to the workspace, etc).
   */
  public enum PassThrough {
    nextflow,
    gcloud,
    gsutil,
    bq
  }
}
