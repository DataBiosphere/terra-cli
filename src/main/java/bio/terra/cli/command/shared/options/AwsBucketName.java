package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --bucket-name option for aws bucket resource commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class AwsBucketName {
  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the AWS bucket, without the prefix. (e.g. 'my-bucket', not 'S3://my-bucket').")
  private String bucketName;

  public String getBucketName() {
    return bucketName;
  }
}
