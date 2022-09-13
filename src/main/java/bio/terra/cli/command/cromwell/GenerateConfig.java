package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra cromwell generate-config" command. */
@Command(
    name = "generate-config",
    description = "Generate cromwell.conf under the user-specified path.")
public class GenerateConfig extends BaseCommand {

  @CommandLine.Option(
      names = "--dir",
      defaultValue = ".",
      required = false,
      description = "Directory to put generated cromwell.conf in. Defaults to current directory.")
  public String dir;

  @Override
  protected void execute() {
    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();
    String petSaEmail = Context.requireUser().getPetSaEmail();

    // Force local process runner, so the generated file exists on local filesystem (as opposed to
    // inside container).
    CommandRunnerOption.LOCAL_PROCESS
        .getRunner()
        .runToolCommand(
            List.of(
                "src/main/java/bio/terra/cli/command/cromwell/generate-config.sh",
                org.apache.commons.io.FilenameUtils.concat(dir, "cromwell.conf"),
                googleProjectId,
                petSaEmail));
  }
}
