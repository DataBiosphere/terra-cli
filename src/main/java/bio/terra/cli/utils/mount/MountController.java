package bio.terra.cli.utils.mount;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import bio.terra.workspace.model.Folder;
import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This class provides utility methods for mounting and unmount workspace resources */
public abstract class MountController {

  // Directory to mount workspace resources under
  private static final Path WORKSPACE_DIR = Paths.get(System.getProperty("user.home"), "workspace");
  private static final String TERRA_FOLDER_ID_PROPERTY_KEY = "terra-folder-id";
  // Command to list mount entries
  private static final String LIST_MOUNT_ENTRIES_COMMAND = "mount";
  // String to look for in mount entry to determine if it is a user created fuse mount
  private static final String FUSE_MOUNT_ENTRY = "fuse";

  protected record MountEntry(String resourceName, String mountPath, String mountDetails) {}

  // Helper method to stub static WORKSPACE_DIR
  public static Path getWorkspaceDir() {
    return WORKSPACE_DIR;
  }

  // Implemented by subclasses per OS
  protected abstract Pattern getMountEntryPattern();

  // Check if the workspace directory exists
  public static boolean workspaceDirExists() {
    return Files.exists(WORKSPACE_DIR) && Files.isDirectory(WORKSPACE_DIR);
  }

  private static void createWorkspaceDir() {
    try {
      Files.createDirectories(WORKSPACE_DIR);
    } catch (IOException e) {
      throw new SystemException("Error creating workspace directory", e);
    }
  }

  protected MountController() {}

  /**
   * Mounts all mountable resources for a given workspace
   *
   * @param disableCache Whether to disable caching for the mounts
   */
  public void mountResources(Boolean disableCache) {
    // Create root workspace directory if it does not exist
    createWorkspaceDir();

    // Fetch workspace resources and folders
    List<Resource> resources = Context.requireWorkspace().listResources();
    Map<UUID, Path> folderPaths = getFolderIdToFolderPathMap();

    // Filter resources by mountable resources and mount them
    resources.stream()
        .filter(this::isMountableResource)
        .forEach(
            r -> {
              Path mountPath = getResourceMountPath(r, folderPaths);
              FileUtils.createDirectories(mountPath);
              BaseMountHandler handler = getMountHandler(r, mountPath, disableCache);
              handler.mount();
            });
  }

  /** Unmount all mountable resources for a given workspace */
  public void unmountResources() {
    // List all mounted directories
    List<String> command = new ArrayList<>(Collections.singleton(LIST_MOUNT_ENTRIES_COMMAND));
    LocalProcessLauncher localProcessLauncher = LocalProcessLauncher.create();
    localProcessLauncher.launchProcess(command, null, null);

    int exitCode = localProcessLauncher.waitForTerminate();
    if (exitCode != 0) {
      throw new SystemException("Failed to query mounted resources.");
    }
    BufferedReader stdout =
        new BufferedReader(new InputStreamReader(localProcessLauncher.getInputStream()));

    stdout
        .lines()
        .map(this::getResourceMountEntry)
        .filter(Objects::nonNull)
        .forEach(mountEntry -> BaseMountHandler.unmount(mountEntry.mountPath));

    FileUtils.deleteEmptyDirectories(getWorkspaceDir());
  }

  /** Helper method to get the mount path for a given resource */
  @VisibleForTesting
  public Path getResourceMountPath(Resource r, Map<UUID, Path> folderPaths) {
    String parentFolderId = r.getProperty(TERRA_FOLDER_ID_PROPERTY_KEY);
    if (parentFolderId != null) {
      return WORKSPACE_DIR.resolve(
          folderPaths.get(UUID.fromString(parentFolderId)).resolve(r.getName()));
    } else {
      return WORKSPACE_DIR.resolve(r.getName());
    }
  }

  /**
   * Helper method to get mountable resources in the workspace, any file containing resource or
   * reference to a file containing resource.
   */
  private boolean isMountableResource(Resource r) {
    if (r.getResourceType() == Resource.Type.GCS_BUCKET) {
      return true;
    }
    if (r.getResourceType() == Resource.Type.GCS_OBJECT) {
      try {
        return ((GcsObject) r).isDirectory();
      } catch (SystemException e) {
        // Pass through GCS objects that are inaccessible to display error on mounted
        // folder later
        return true;
      }
    }
    return false;
  }

  /**
   * Helper method to parse a resource mount entry to be unmounted.
   *
   * @param mountOutputLine mount entry line from a list of mounded devices
   * @return MountEntry object if the mount entry is a user created fuse mount, null otherwise
   */
  private MountEntry getResourceMountEntry(String mountOutputLine) {
    Matcher matcher = getMountEntryPattern().matcher(mountOutputLine);
    if (matcher.find()) {
      MountEntry mountEntry = getMountEntry(matcher);
      if (mountEntry.mountPath.contains(getWorkspaceDir().toString())
          && mountEntry.mountDetails.contains(FUSE_MOUNT_ENTRY)) return mountEntry;
    }
    return null;
  }

  /**
   * Helper method to get workspace folder paths. Used to build the mount paths for mountable
   * resources.
   *
   * @return A map of folder IDs to folder paths.
   */
  @VisibleForTesting
  public Map<UUID, Path> getFolderIdToFolderPathMap() {
    List<Folder> folders = Context.requireWorkspace().listFolders();
    Map<UUID, Path> folderPaths = new HashMap<>();

    for (Folder folder : folders) {
      Path path = Paths.get(folder.getDisplayName());

      // Recursively prepend parent folder names to path
      UUID parentId = folder.getParentFolderId();
      while (parentId != null) {
        // Get parent folder
        Folder parent = null;
        for (Folder f : folders) {
          if (f.getId().equals(parentId)) {
            parent = f;
            break;
          }
        }
        // Prepend parent folder name to path
        if (parent != null) {
          path = Paths.get(parent.getDisplayName()).resolve(path);
          parentId = parent.getParentFolderId();
        }
      }
      folderPaths.put(folder.getId(), path);
    }
    return folderPaths;
  }

  /**
   * Parses a regex line match result into a MountEntry object.
   *
   * @param matcher Matcher object
   * @return A MountEntry object containing the bucketName, mountPath, and mountDetails for the
   *     mount entry.
   */
  private MountEntry getMountEntry(Matcher matcher) {
    String bucketName = matcher.group(1);
    String mountPath = matcher.group(2).trim();
    String mountedDetails = matcher.group(3);
    return new MountEntry(bucketName, mountPath, mountedDetails);
  }

  /**
   * Get the mount handler for a resource.
   *
   * @param r resource to get the mount handler for
   * @param mountPoint mount point path for the resource
   * @return mount handler for the resource
   */
  public BaseMountHandler getMountHandler(Resource r, Path mountPoint, Boolean disableCache) {
    return switch (r.getResourceType()) {
      case GCS_BUCKET -> new GcsFuseMountHandler((GcsBucket) r, mountPoint, disableCache);
      case GCS_OBJECT -> new GcsFuseMountHandler((GcsObject) r, mountPoint, disableCache);
      default -> throw new SystemException("Unsupported resource type: " + r.getResourceType());
    };
  }
}
