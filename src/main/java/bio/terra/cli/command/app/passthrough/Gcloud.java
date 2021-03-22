package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.command.baseclasses.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(
    name = "gcloud",
    description = "Use the gcloud tool in the Terra workspace.",
    hidden = true)
public class Gcloud extends BaseCommand<Void> {

  @CommandLine.Unmatched private List<String> cmdArgs;

  @Override
  protected Void execute() {
    String fullCommand = DockerAppsRunner.buildFullCommand("gcloud", cmdArgs);

    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand);

    return null;
  }
}
