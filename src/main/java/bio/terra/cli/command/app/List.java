package bio.terra.cli.command.app;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app list" command. */
@Command(name = "list", description = "List the supported applications.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Print out a list of all the supported apps. */
  @Override
  protected void execute() {
    java.util.List<String> returnValue =
        Arrays.asList(PassThrough.values()).stream()
            .sorted(Comparator.comparing(PassThrough::name))
            .map(passthrough -> passthrough.toString())
            .collect(Collectors.toList());
    formatOption.printReturnValue(returnValue, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<String> returnValue) {
    OUT.println(
        "Call any of the supported applications listed below, by prefixing it with 'terra' (e.g. terra gsutil ls, terra nextflow run hello)\n");
    for (String app : returnValue) {
      OUT.println("  " + app);
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
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
