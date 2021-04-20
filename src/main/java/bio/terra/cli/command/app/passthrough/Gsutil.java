package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.AppsRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gsutil" command. */
@Command(
    name = "gsutil",
    description = "Use the gsutil tool in the Terra workspace.",
    hidden = true)
public class Gsutil extends BaseCommand {

  @CommandLine.Unmatched private List<String> cmdArgs;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    String fullCommand = AppsRunner.buildFullCommand("gsutil", cmdArgs);

    // no need for any special setup or teardown logic since gsutil is already initialized when the
    // container starts
    new AppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
