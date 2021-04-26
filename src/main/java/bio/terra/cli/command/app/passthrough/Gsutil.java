package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.DockerCommandRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gsutil" command. */
@Command(name = "gsutil", description = "Call gsutil in the Terra workspace.")
public class Gsutil extends BaseCommand {

  @CommandLine.Unmatched private List<String> cmdArgs;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    String fullCommand = DockerCommandRunner.buildFullCommand("gsutil", cmdArgs);

    // no need for any special setup or teardown logic since gsutil is already initialized when the
    // container starts
    new DockerCommandRunner(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
