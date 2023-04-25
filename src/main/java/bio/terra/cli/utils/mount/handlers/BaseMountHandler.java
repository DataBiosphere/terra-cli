package bio.terra.cli.utils.mount.handlers;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for mounting a resource to a mount point. This class is extended by specific
 * resource mount handlers. Subclasses need to implement the mount and unmount methods.
 */
public abstract class BaseMountHandler {

  protected static Logger logger = LoggerFactory.getLogger(BaseMountHandler.class);
  protected Path mountPoint;
  protected boolean disableCache;

  @VisibleForTesting
  public static void setLogger(Logger newLogger) {
    logger = newLogger;
  }

  protected BaseMountHandler(Path mountPoint, Boolean disableCache) {
    this.mountPoint = mountPoint;
    this.disableCache = disableCache;
  }

  /** Mounts the resource at the mount point. */
  public abstract int mount() throws UserActionableException, SystemException;

  /**
   * Unmounts the resource at the mount point.
   *
   * @param mountPath the path of the bucket.
   * @throws UserActionableException if the mount entry is being used by another process.
   * @throws SystemException if the unmount fails because there is no existing mount entry or any
   *     other error.
   */
  public static void unmount(String mountPath) throws UserActionableException, SystemException {
    // Build unmount command
    List<String> command = new java.util.ArrayList<>(getUnmountCommand());
    command.add(mountPath);

    // Run unmount command
    LocalProcessLauncher localProcessLauncher = LocalProcessLauncher.create();
    localProcessLauncher.launchProcess(command, null, null);
    int exitCode = localProcessLauncher.waitForTerminate();

    // Throw an error if the mount directory is being used by another process
    // Ignore errors related to the mount point not being mounted
    String errorMessage = localProcessLauncher.getErrorString();
    if (exitCode != 0) {
      if (!isNotMountedError(errorMessage)) {
        logger.error(errorMessage);
        throw new UserActionableException(
            "Failed to unmount "
                + mountPath
                + ". Make sure that the mount point is not being used by other processes.");
      }
    } else {
      logger.info("Unmounted " + mountPath);
    }
  }

  /**
   * Returns the appropriate unmount command based on the current operating system.
   *
   * @return The base unmount command.
   * @throws UserActionableException if the current operating system is not supported for unmounting
   *     GCS bucket.
   */
  private static List<String> getUnmountCommand() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return List.of("umount");
    } else if (os.contains("linux")) {
      return List.of("fusermount", "-u");
    }
    throw new UserActionableException("Unsupported OS for unmounting GCS bucket: " + os);
  }

  /**
   * Check if the unmount error is because the bucket is already not mounted.
   *
   * @param errorString the error string to check
   * @return true if the error stream contains the given string
   */
  private static boolean isNotMountedError(String errorString) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return errorString.contains("not currently mounted");
    } else if (os.contains("linux")) {
      return errorString.contains("not found in");
    }
    return false;
  }

  protected void addPermissionErrorToMountPoint() {
    addErrorStateToMountPoint("_NO_ACCESS");
  }

  protected void addNotFoundErrorToMountPoint() {
    addErrorStateToMountPoint("_NOT_FOUND");
  }

  protected void addErrorStateToMountPoint() {
    addErrorStateToMountPoint("_MOUNT_FAILED");
  }

  /**
   * Appends an error string to the mount point directory. This is done by renaming the mount point
   * to mountPoint_errorString.
   *
   * @param errorString the error string.
   */
  private void addErrorStateToMountPoint(String errorString) {
    // Add error state to mount point
    Path pathWithErrorString = mountPoint.resolveSibling(mountPoint.toAbsolutePath() + errorString);
    try {
      Files.move(mountPoint, pathWithErrorString);
    } catch (IOException e) {
      throw new SystemException(
          "Failed to set error state to mounted resource at: " + mountPoint, e);
    }
  }
}
