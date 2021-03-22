package bio.terra.cli.command.app;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import java.util.Arrays;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app list" command. */
@Command(name = "list", description = "List the supported applications.")
public class List extends CommandWithFormatOptions<java.util.List<String>> {

  @Override
  protected java.util.List<String> execute() {
    return Arrays.asList(PassThrough.values()).stream()
        .map(passthrough -> passthrough.toString())
        .collect(Collectors.toList());
  }

  @Override
  protected void printText(java.util.List<String> returnValue) {
    out.println(
        "Call any of the supported applications listed below, by prefixing it with 'terra' (e.g. terra gsutil ls, terra nextflow run hello)\n");
    for (PassThrough app : PassThrough.values()) {
      System.out.println("  " + app);
    }
  }

  /**
   * This enum specifies the list returned by the 'terra app list' command. These commands are
   * hidden top-level commands that can be called in a pass-through manner, meaning the user can
   * call them as if they were running the commands locally, except with a "terra" prefix. This
   * prefix means that the command will run within the context of a Terra workspace (e.g. in the
   * backing Google project, with data references scoped to the workspace, etc).
   */
  public enum PassThrough {
    nextflow,
    gcloud,
    gsutil,
    bq
  }
}
