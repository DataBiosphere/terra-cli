package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.CromwellPath;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra cromwell generate-config" command. */
@Command(
    name = "generate-config",
    description = "Autogenerate a cromwell.conf under the user specified path.")
public class GenerateConfig extends BaseCommand {

  @CommandLine.Mixin CromwellPath cromwellPath;

  @Override
  protected void execute() {
    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();
    String petSaEmail = Context.requireUser().getPetSaEmail();

    // Use {WORKSPACE_BUCKET} as a space holder now, until the workspace created with bucket.
    Context.getConfig()
        .getCommandRunnerOption()
        .getRunner()
        .runToolCommand(
            List.of(
                "src/main/java/bio/terra/cli/command/cromwell/generate-config.sh",
                Optional.ofNullable(cromwellPath.path).orElse("cromwell.conf"),
                googleProjectId,
                "{WORKSPACE_BUCKET}",
                petSaEmail));
  }
}
