package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --new-bucket-name option for aws bucket resource commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class AwsBucketNewName {
  @CommandLine.Option(
      names = "--new-bucket-name",
      description =
          "New name of the AWS bucket, without the prefix. (e.g. 'my-bucket', not 'S3://my-bucket').")
  private String newBucketName;

  public String getNewBucketName() {
    return newBucketName;
  }
}
