package bio.terra.cli.utils.mount.handlers;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.exception.SystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** This class handles mounting a GCS bucket or prefix object using the GCS FUSE driver. */
public class GcsFuseMountHandler extends BaseMountHandler {

  private static final String FUSE_MOUNT_COMMAND = "gcsfuse";
  private final String bucketName;
  private @Nullable String subDir;

  public GcsFuseMountHandler(GcsBucket gcsBucket, Path mountPoint, Boolean disableCache) {
    super(mountPoint, disableCache);
    this.bucketName = gcsBucket.getBucketName();
  }

  public GcsFuseMountHandler(GcsObject gcsObject, Path mountPoint, Boolean disableCache) {
    super(mountPoint, disableCache);
    this.bucketName = gcsObject.getBucketName();
    this.subDir = gcsObject.getObjectName();
  }

  /** Implements the mount method for a GCS bucket or prefix object. */
  public int mount() throws SystemException {
    // Build mount command
    List<String> command = new ArrayList<>(List.of(FUSE_MOUNT_COMMAND));
    if (disableCache) {
      command.addAll(List.of("--stat-cache-ttl", "0s", "--type-cache-ttl", "0s"));
    }
    if (subDir != null) {
      command.addAll(List.of("--only-dir", subDir));
    }
    command.addAll(List.of(bucketName, mountPoint.toString()));

    // Run mount command
    LocalProcessLauncher localProcessLauncher = LocalProcessLauncher.create();
    localProcessLauncher.launchProcess(command, null);
    int exitCode = localProcessLauncher.waitForTerminate();

    // Add errors to the mount point directories if the mount fails
    String errorMessage = localProcessLauncher.getErrorString();
    String bucketOutputName = subDir != null ? bucketName + "/" + subDir : bucketName;
    if (exitCode != 0) {
      addErrorToMountPoint(errorMessage, bucketOutputName);
      logger.error(errorMessage);
      return exitCode;
    }

    logger.info("Mounted " + bucketOutputName);
    return 0;
  }

  /**
   * Append appropriate error string to the mount point directory if the mount fails.
   *
   * @param errorMessage gcsfuse error message to parse
   * @param bucketOutputName bucket and object path to display in log message.
   */
  public void addErrorToMountPoint(String errorMessage, String bucketOutputName) {
    logger.error(errorMessage);
    if (errorMessage.contains("forbidden")) {
      addPermissionErrorToMountPoint();
      logger.info("Insufficient permissions. Unable to access GCS bucket " + bucketOutputName);
    } else if (errorMessage.contains("bucket doesn't exist")) {
      addNotFoundErrorToMountPoint();
      logger.info("GCS bucket not found: " + bucketOutputName);
    } else {
      addErrorStateToMountPoint();
      logger.info("Failed to mount GCS bucket " + bucketOutputName);
    }
  }
}
