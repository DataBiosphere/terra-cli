package bio.terra.cli.command.cromwell;

import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.command.shared.BaseCommand;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  // We create an exclusive ArgGroup because we require one of either
  // workspace_bucket_name or google_bucket_name.
  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  WorkspaceOrGoogleBucketName workspace_or_google_bucket_name;

  static class WorkspaceOrGoogleBucketName {
    @CommandLine.Option(
        names = "--workspace-bucket-name",
        required = true,
        description = "Terra workspace bucket used as the root of Cromwell workflow execution.")
    public String workspace_bucket_name;

    @CommandLine.Option(
        names = "--google-bucket-name",
        required = true,
        description =
            "Google Cloud Storage bucket used as the root of Cromwell workflow execution. For example: gs://bucket-name.")
    public String google_bucket_name;
  }

  @Override
  protected void execute() {
    // Get the absolute path for the generate-cromwell-config.sh file.
    String absolutePath = "";
    try {
      File file = File.createTempFile("generate-cromwell-config", ".sh");
      Path path = file.toPath();
      InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream("configs/generate-cromwell-config.sh");
      // Copy the content in script and grant read, write, execute permission.
      Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
      absolutePath = path.toString();
    } catch (IOException e) {
      OUT.println("Failed to write to cromwell config file");
      return;
    }

    // TODO(TERRA-210) Add AWS specs into cromwell config
    String googleProjectId = Context.requireWorkspace().getRequiredGoogleProjectId();
    String petSaEmail = Context.requireUser().getPetSaEmail();

    String googleBucket;
    if (workspace_or_google_bucket_name.workspace_bucket_name == null) {
      googleBucket = workspace_or_google_bucket_name.google_bucket_name;
      if (!googleBucket.startsWith("gs://")) googleBucket = "gs://" + googleBucket;
    } else {
      Resource resource =
          Context.requireWorkspace()
              .getResource(workspace_or_google_bucket_name.workspace_bucket_name);
      googleBucket = ((GcsBucket) resource).resolve(/*includeUrlPrefix=*/ true);
    }

    // Force local process runner, so the generated file exists on local filesystem (as opposed to
    // inside container).
    CommandRunnerOption.LOCAL_PROCESS
        .getRunner()
        .runToolCommand(
            List.of(
                absolutePath,
                org.apache.commons.io.FilenameUtils.concat(dir, "cromwell.conf"),
                googleProjectId,
                petSaEmail,
                googleBucket));
    try {
      Files.deleteIfExists(Paths.get(absolutePath));
    } catch (IOException e) {
      OUT.println("Failed to delete the cromwell config temporary file");
    }
  }
}
