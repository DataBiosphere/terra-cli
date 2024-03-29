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

  // Base gcsfuse mount command.
  // with --implicit-dirs flag to show directories on the path to an object with prefix so that the
  // object can be accessible.
  private static final List<String> FUSE_MOUNT_COMMAND = List.of("gcsfuse", "--implicit-dirs");
  private final String bucketName;
  private @Nullable String subDir;

  public GcsFuseMountHandler(
      GcsBucket gcsBucket, Path mountPoint, boolean disableCache, boolean readOnly) {
    super(mountPoint, disableCache, readOnly);
    this.bucketName = gcsBucket.getBucketName();
  }

  public GcsFuseMountHandler(
      GcsObject gcsObject, Path mountPoint, boolean disableCache, boolean readOnly) {
    super(mountPoint, disableCache, readOnly);
    this.bucketName = gcsObject.getBucketName();
    this.subDir = gcsObject.getObjectName();
  }

  /** Implements the mount method for a GCS bucket or prefix object. */
  public int mount() throws SystemException {
    // Build mount command
    List<String> command = new ArrayList<>(FUSE_MOUNT_COMMAND);
    if (disableCache) {
      command.addAll(List.of("--stat-cache-ttl", "0s", "--type-cache-ttl", "0s"));
    }
    if (subDir != null) {
      command.addAll(List.of("--only-dir", subDir));
    }
    if (readOnly) {
      command.addAll(List.of("-o", "ro", "--file-mode", "444", "--dir-mode", "555"));
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
      addErrorStateToMountPoint(BaseMountHandler.PERMISSION_ERROR);
      logger.info("Insufficient permissions. Unable to access GCS bucket " + bucketOutputName);
    } else if (errorMessage.contains("bucket doesn't exist")) {
      addErrorStateToMountPoint(BaseMountHandler.NOT_FOUND_ERROR);
      logger.info("GCS bucket not found: " + bucketOutputName);
    } else {
      addErrorStateToMountPoint(BaseMountHandler.MOUNT_FAILED_ERROR);
      logger.info("Failed to mount GCS bucket " + bucketOutputName);
    }
  }
}
