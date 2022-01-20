package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --bucket-name option for gcs bucket resource commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GcsBucketName {

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket').")
  private String bucketName;

  public String getBucketName() {
    return bucketName;
  }
}
