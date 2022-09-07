package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.List;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app execute" command. */
@Command(
    name = "generate-config",
    description = "Autogenerate a cromwell.conf under /home/jupyter/cromwell")
public class GenerateConfig extends BaseCommand {

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();

    // Use {WORKSPACE_BUCKET} as a space holder now, until the workspace created with bucket.
    Context.getConfig()
        .getCommandRunnerOption()
        .getRunner()
        .runToolCommand(
            List.of(
                "src/main/java/bio/terra/cli/command/cromwell/generate.sh",
                "{WORKSPACE_BUCKET}",
                googleProjectId));
  }
}
