package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(
    name = "nextflow",
    description = "Use the nextflow tool in the Terra workspace.",
    hidden = true)
public class Nextflow extends BaseCommand {

  @CommandLine.Unmatched private List<String> cmdArgs;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("NXF_MODE", "google");

    String fullCommand = DockerAppsRunner.buildFullCommand("nextflow", cmdArgs);
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand, envVars);
  }
}
