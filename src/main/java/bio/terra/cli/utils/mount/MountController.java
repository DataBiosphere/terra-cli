package bio.terra.cli.utils.mount;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import bio.terra.workspace.model.Folder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** This class provides utility methods for mounting and unmount workspace resources */
public abstract class MountController {

  // Directory to mount workspace resources under
  private static final Path WORKSPACE_DIR =
      Paths.get(Context.getContextDir().toString(), "workspace");
  private static final String TERRA_FOLDER_ID_PROPERTY_KEY = "terra-folder-id";
  // Command to list mount entries
  private static final String LIST_MOUNT_ENTRIES_COMMAND = "mount";
  // String to look for in mount entry to determine if it is a user fuse mount
  private static final String FUSE_MOUNT_ENTRY = "fuse";

  protected record MountEntry(String resourceName, String mountPath, String mountDetails) {}

  protected abstract Pattern getMountEntryPattern();

  // Check if the workspace directory exists
  public static boolean workspaceDirExists() {
    return Files.exists(WORKSPACE_DIR) && Files.isDirectory(WORKSPACE_DIR);
  }

  protected MountController() {}

  /**
   * Mounts all mountable resources for a given workspace
   *
   * @param ws workspace context
   */
  public void mountResources(Workspace ws, Boolean disableCache) {
    Map<UUID, Path> resourceMountPaths = getResourceMountPaths();

    // Create root workspace directory if it does not exist
    try {
      Files.createDirectories(WORKSPACE_DIR);
    } catch (IOException e) {
      throw new SystemException("Error creating workspace directory", e);
    }

    // Create directories for each resource mount point
    createResourceDirectories(new ArrayList<>(resourceMountPaths.values()));

    // Mount each resource
    resourceMountPaths.forEach(
        (id, mountPath) -> {
          Resource r = ws.getResource(id);
          BaseMountHandler handler = getMountHandler(r, mountPath, disableCache);
          handler.mount();
        });
  }

  /** Unmount all mountable resources for a given workspace */
  public void unmountResources() {
    // Parse `mount` output to get mount paths
    List<String> command = new ArrayList<>(Collections.singleton(LIST_MOUNT_ENTRIES_COMMAND));
    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    localProcessLauncher.launchProcess(command, null, null);

    int exitCode = localProcessLauncher.waitForTerminate();
    if (exitCode != 0) {
      throw new SystemException("Failed to query mounted resources.");
    }
    BufferedReader stdout =
        new BufferedReader(new InputStreamReader(localProcessLauncher.getInputStream()));

    Pattern mountEntryPattern = getMountEntryPattern();
    stdout
        .lines()
        .map(
            line -> {
              Matcher matcher = mountEntryPattern.matcher(line);
              if (matcher.find()) {
                return matcher;
              }
              return null;
            })
        .filter(Objects::nonNull)
        .map(this::getMountEntry)
        .filter(mountEntry -> mountEntry.mountPath.contains(WORKSPACE_DIR.toString()))
        .filter(mountEntry -> mountEntry.mountDetails.contains(FUSE_MOUNT_ENTRY))
        .forEach(mountEntry -> BaseMountHandler.unmount(mountEntry.resourceName));

    deleteEmptyResourceDirectories();
  }

  /**
   * Get the mount paths for every mountable resource in the workspace. Mount paths are relative to
   * WORKSPACE_DIR.
   *
   * @return A map of resource IDs to mount paths.
   */
  private Map<UUID, Path> getResourceMountPaths() {
    List<Resource> resources = getMountableResources();

    Map<UUID, Path> folderPaths = getFolderIdToFolderPathMap();
    Map<UUID, Path> resourceMountPaths = new HashMap<>();

    for (Resource resource : resources) {
      String parentFolderId = resource.getProperty(TERRA_FOLDER_ID_PROPERTY_KEY);
      if (parentFolderId != null) {
        Path mountPath =
            WORKSPACE_DIR.resolve(
                folderPaths.get(UUID.fromString(parentFolderId)).resolve(resource.getName()));
        resourceMountPaths.put(resource.getId(), mountPath);
      } else {
        resourceMountPaths.put(resource.getId(), WORKSPACE_DIR.resolve(resource.getName()));
      }
    }
    return resourceMountPaths;
  }

  /**
   * Helper method to get mountable resources in the workspace, any file containing resource or
   * reference to a file containing resource.
   */
  private List<Resource> getMountableResources() {
    return Context.requireWorkspace().listResources().stream()
        .filter(
            r -> {
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
            })
        .toList();
  }

  /**
   * Helper method to get workspace folder paths. Used to build the mount paths for mountable
   * resources.
   *
   * @return A map of folder IDs to folder paths.
   */
  private Map<UUID, Path> getFolderIdToFolderPathMap() {
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
   * Creates local directories for all the given resource paths.
   *
   * @param paths A list of resource paths to create directories for.
   * @throws SystemException If the creation of the directories failed.
   */
  public static void createResourceDirectories(List<Path> paths) throws SystemException {
    for (Path path : paths) {
      if (!path.toFile().exists() && !path.toFile().mkdirs()) {
        throw new SystemException("Failed to create directory: " + path);
      }
    }
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
   * Recursively deletes empty subdirectories in the WORKSPACE_DIR, excluding the WORKSPACE_DIR
   * itself.
   *
   * <p>Throws UserActionableException if a non-empty directory is encountered. Throws
   * SystemException if there is an error during deletion.
   */
  private static void deleteEmptyResourceDirectories() {
    // Explore WORKSPACE_DIR in reverse DFS order
    try (Stream<Path> stream = Files.walk(WORKSPACE_DIR)) {
      stream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .filter(File::isDirectory)
          .filter(dir -> !dir.equals(WORKSPACE_DIR.toFile())) // exclude WORKSPACE_DIR
          .filter(
              dir -> {
                if (Objects.requireNonNull(dir.listFiles()).length == 0) {
                  return true;
                } else {
                  throw new UserActionableException(
                      "Cannot delete non-empty directory: "
                          + dir
                          + ". Please move the files in this directory and rerun the command.");
                }
              })
          .forEach(
              dir -> {
                if (!dir.delete()) {
                  throw new SystemException("Failed to delete empty directory: " + dir);
                }
              });
    } catch (IOException e) {
      throw new UserActionableException(
          "Failed to open directory: " + WORKSPACE_DIR + ". Create ", e);
    }
  }

  /**
   * Get the mount handler for a resource.
   *
   * @param r resource to get the mount handler for
   * @param mountPoint mount point path for the resource
   * @return mount handler for the resource
   */
  public static BaseMountHandler getMountHandler(
      Resource r, Path mountPoint, Boolean disableCache) {
    return switch (r.getResourceType()) {
      case GCS_BUCKET -> new GcsFuseMountHandler((GcsBucket) r, mountPoint, disableCache);
      case GCS_OBJECT -> new GcsFuseMountHandler((GcsObject) r, mountPoint, disableCache);
      default -> throw new SystemException("Unsupported resource type: " + r.getResourceType());
    };
  }
}
