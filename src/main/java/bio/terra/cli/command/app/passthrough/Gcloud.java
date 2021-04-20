package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.AppsRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(
    name = "gcloud",
    description = "Use the gcloud tool in the Terra workspace.",
    hidden = true)
public class Gcloud extends BaseCommand {

  @CommandLine.Unmatched private List<String> cmdArgs;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    String fullCommand = AppsRunner.buildFullCommand("gcloud", cmdArgs);

    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    new AppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand);
  }
}
