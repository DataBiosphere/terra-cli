package bio.terra.cli.command.shared.options;

import javax.annotation.Nullable;
import picocli.CommandLine;

/**
 * Command helper class that defines the --new-bucket-name option for gcs bucket resource commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GcsBucketResourceUpdate {

  @CommandLine.Option(
      names = "--new-bucket-name",
      description =
          "New name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private @Nullable String newBucketName;

  public @Nullable String getNewBucketName() {
    return newBucketName;
  }
}
