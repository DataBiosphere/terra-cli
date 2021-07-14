package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import picocli.CommandLine;

/**
 * Command helper class that defines the --storage option for `terra resources` commands that handle
 * GCS bucket controlled resources.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GcsBucketStorageClass {
  @CommandLine.Option(
      names = "--storage",
      description =
          "Storage class (https://cloud.google.com/storage/docs/storage-classes): ${COMPLETION-CANDIDATES}.")
  public GcpGcsBucketDefaultStorageClass storageClass;

  public boolean isDefined() {
    return storageClass != null;
  }
}
