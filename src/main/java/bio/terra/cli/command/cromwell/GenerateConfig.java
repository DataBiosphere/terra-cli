package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
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
    // Get the absolute path for the generate-cromwell-config.sh file.
    URL res = getClass().getClassLoader().getResource("configs/generate-cromwell-config.sh");
    File file = Paths.get(res.getPath()).toFile();
    String absolutePath = file.getAbsolutePath();

    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();
    String petSaEmail = Context.requireUser().getPetSaEmail();

    // Force local process runner, so the generated file exists on local filesystem (as opposed to
    // inside container).
    CommandRunnerOption.LOCAL_PROCESS
        .getRunner()
        .runToolCommand(
            List.of(
                absolutePath,
                org.apache.commons.io.FilenameUtils.concat(dir, "cromwell.conf"),
                googleProjectId,
                petSaEmail));
  }
}
