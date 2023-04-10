package bio.terra.cli.businessobject.mounthandler;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for mounting a resource to a mount point. This class is extended by specific
 * resource mount handlers. Subclasses need to implement the mount and unmount methods.
 */
public abstract class ResourceMountHandler {

  Logger logger = LoggerFactory.getLogger(this.getClass());
  protected Path mountPoint;

  public ResourceMountHandler(Path mountPoint) {
    this.mountPoint = mountPoint;
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

  /**
   * Checks if the provided error string is present in the given input stream. Used to differentiate
   * the different errors that all return exit code 1.
   *
   * @param errorStream the input stream to search for the error substring
   * @param error the error string to search for
   * @return true if the error is present in the input stream, false otherwise
   * @throws SystemException if there was an error while reading the error stream
   */
  protected boolean errorContains(InputStream errorStream, String error) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
      return reader.lines().anyMatch(line -> line.toLowerCase().contains(error));
    } catch (IOException e) {
      throw new SystemException("Failed to read error stream", e);
    }
  }

  protected void addPermissionErrorToMountPoint() {
    addErrorStateToMountPoint("_NO_ACCESS");
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
