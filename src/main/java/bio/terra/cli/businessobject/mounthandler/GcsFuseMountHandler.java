package bio.terra.cli.businessobject.mounthandler;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles mounting a GCS bucket or prefix object using the GCS FUSE driver.
 */
public class GcsFuseMountHandler extends ResourceMountHandler {
  Logger logger = LoggerFactory.getLogger(GcsFuseMountHandler.class);
  protected final String FUSE_MOUNT_COMMAND = "gcsfuse";
  protected String bucketName;
  protected String subDir;

  public GcsFuseMountHandler(GcsBucket gcsBucket, Path mountPoint, Boolean disableCache) {
    super(mountPoint, disableCache);
    this.bucketName = gcsBucket.getBucketName();
  }

  public GcsFuseMountHandler(GcsObject gcsObject, Path mountPoint, Boolean disableCache) {
    super(mountPoint, disableCache);
    this.bucketName = gcsObject.getBucketName();
    this.subDir = gcsObject.getObjectName();
  }

  public void mount() throws SystemException {
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
    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    Process p = localProcessLauncher.launchSilentProcess(command, null, null);

    // Add errors to the mount point directories if the mount fails
    String errorMessage = localProcessLauncher.getErrorString();
    String bucketOutputName = subDir != null ? bucketName + "/" + subDir : bucketName;
    if (p.exitValue() != 0) {
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
    } else {
      logger.info("Mounted " + bucketOutputName);
    }
  }

  public void unmount() throws SystemException {
    // Build unmount command
    List<String> command = Arrays.asList(getUnmountCommand(), mountPoint.toString());

    // Run unmount command
    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    Process p = localProcessLauncher.launchSilentProcess(command, null, null);

    // Throw an error if the mount directory is being used by another process
    // Ignore errors related to the mount point not being mounted
    String errorMessage = localProcessLauncher.getErrorString();
    if (p.exitValue() != 0) {
      if (!isNotMountedError(errorMessage)) {
        throw new UserActionableException(
            "Failed to unmount "
                + mountPoint
                + ". Make sure that the mount point is not being used by other processes.");
      }
    } else {
      logger.info("Unmounted GCS bucket " + bucketName);
    }
  }

  /**
   * Returns the appropriate unmount command based on the current operating system.
   *
   * @return The unmount command as a String.
   * @throws UserActionableException if the current operating system is not supported for unmounting
   *     GCS bucket.
   */
  private String getUnmountCommand() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return "umount";
    } else if (os.contains("linux")) {
      return "fusermount -u";
    }
    throw new UserActionableException("Unsupported OS for unmounting GCS bucket: " + os);
  }

  /**
   * Check if the unmount error is because the bucket is already not mounted.
   *
   * @param errorString the error string to check
   * @return true if the error stream contains the given string
   */
  private boolean isNotMountedError(String errorString) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return errorString.contains("not currently mounted");
    } else if (os.contains("linux")) {
      return errorString.contains("not found in");
    }
    return false;
  }
}
