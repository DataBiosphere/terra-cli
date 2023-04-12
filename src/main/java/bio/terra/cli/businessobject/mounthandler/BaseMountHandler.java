package bio.terra.cli.businessobject.mounthandler;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for mounting a resource to a mount point. This class is extended by specific
 * resource mount handlers. Subclasses need to implement the mount and unmount methods.
 */
public abstract class BaseMountHandler {

  protected static Logger logger = LoggerFactory.getLogger(BaseMountHandler.class);
  protected Path mountPoint;
  protected Boolean disableCache;

  public BaseMountHandler(Path mountPoint, Boolean disableCache) {
    this.mountPoint = mountPoint;
    this.disableCache = disableCache;
  }

  /**
   * Mounts the resource at the mount point.
   *
   * @throws SystemException if the mount fails
   */
  public abstract void mount() throws UserActionableException, SystemException;

  /**
   * Unmounts the resource at the mount point.
   *
   * @throws SystemException if the unmount fails
   */
  public abstract void unmount() throws UserActionableException, SystemException;

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
   * Adds an error state to the mount point.This is done by renaming the mount point to
   * mountPoint_errorString.
   *
   * @param errorString the error string to add to the mount point
   */
  private void addErrorStateToMountPoint(String errorString) {
    // Add error state to mount point
    Path newPath = mountPoint.resolveSibling(mountPoint.toAbsolutePath() + errorString);
    try {
      Files.move(mountPoint, newPath);
    } catch (IOException e) {
      throw new SystemException(
          "Failed to add error state to mounted resource at: " + mountPoint, e);
    }
  }
}
