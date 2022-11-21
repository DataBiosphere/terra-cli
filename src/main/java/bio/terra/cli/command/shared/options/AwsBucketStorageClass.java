package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.AwsBucketDefaultStorageClass;
import picocli.CommandLine;

/**
 * Command helper class that defines the --storage option for `terra resource` commands that handle
 * AWS bucket controlled resources.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class AwsBucketStorageClass {
  @CommandLine.Option(
      names = "--storage",
      description =
          "Storage class (https://aws.amazon.com/s3/storage-classes): ${COMPLETION-CANDIDATES}.")
  public AwsBucketDefaultStorageClass storageClass;

  public boolean isDefined() {
    return storageClass != null;
  }
}
