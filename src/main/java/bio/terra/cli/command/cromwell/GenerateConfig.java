package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
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
    String absolutePath = "";
    try {
      Path file = Files.createTempFile(null, ".sh");
      InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream("configs/generate-cromwell-config.sh");
      // Copy the content in script and grant read, write, execute permission.
      Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
      Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxrwxrwx"));
      absolutePath = file.toString();
    } catch (IOException e) {
      OUT.println("Failed to write to cromwell config file");
    }

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
