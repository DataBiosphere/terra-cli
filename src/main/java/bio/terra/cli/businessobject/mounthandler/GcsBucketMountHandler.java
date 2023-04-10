package bio.terra.cli.businessobject.mounthandler;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsBucketMountHandler extends ResourceMountHandler {

  Logger logger = LoggerFactory.getLogger(GcsBucketMountHandler.class);

  private static final String FUSE_MOUNT_COMMAND = "gcsfuse";

  private final GcsBucket gcsBucket;

  public GcsBucketMountHandler(GcsBucket resource, Path mountPoint) {
    super(mountPoint);
    this.gcsBucket = resource;
  }

  public void mount() throws SystemException {
    List<String> command =
        Arrays.asList(FUSE_MOUNT_COMMAND, gcsBucket.getBucketName(), mountPoint.toString());

    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    Process p = localProcessLauncher.launchSilentProcess(command);

    // Add errors to the mount point directories if the mount fails
    if (p.exitValue() != 0 && errorContains(localProcessLauncher.getErrorStream(), "permission")) {
      logger.info("Permission error when mounting GCS bucket " + gcsBucket.getBucketName());
      addPermissionErrorToMountPoint();
    } else if (p.exitValue() != 0) {
      logger.info("Failed to mount GCS bucket " + gcsBucket.getBucketName());
      addErrorStateToMountPoint();
    } else {
      logger.info("Mounted GCS bucket " + gcsBucket.getBucketName());
    }
  }

  public void unmount() throws SystemException {
    List<String> command = Arrays.asList(getUnmountCommand(), mountPoint.toString());

    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    Process p = localProcessLauncher.launchSilentProcess(command);

    // Throw an error if the mount directory is being used by another process
    // Ignore errors related to the mount point not being mounted
    if (p.exitValue() != 0) {
      if (!isNotMountedError(localProcessLauncher.getErrorStream())) {
        throw new UserActionableException(
            "Failed to unmount "
                + mountPoint
                + ". Make sure that the mountpoint is not being used by other processes.");
      }
    } else {
      logger.info("Unmounted GCS bucket " + gcsBucket.getBucketName());
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
   * @param errorStream the error stream to check
   * @return true if the error stream contains the given string
   */
  private boolean isNotMountedError(InputStream errorStream) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return errorContains(errorStream, "not currently mounted");
    } else if (os.contains("linux")) {
      return errorContains(errorStream, "not found in");
    }
    return false;
  }
}
