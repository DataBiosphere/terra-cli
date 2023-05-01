package bio.terra.cli.utils.mount;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.mount.handlers.BaseMountHandler;
import bio.terra.cli.utils.mount.handlers.GcsFuseMountHandler;
import bio.terra.workspace.model.Folder;
import bio.terra.workspace.model.StewardshipType;
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
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** This class provides utility methods for mounting and unmount workspace resources */
public abstract class MountController {

  // Directory to mount workspace resources under
  private static final Path WORKSPACE_DIR = Paths.get(System.getProperty("user.home"), "workspace");
  private static final String TERRA_FOLDER_ID_PROPERTY_KEY = "terra-folder-id";
  // Command to list mount entries
  private static final String LIST_MOUNT_ENTRIES_COMMAND = "mount";
  // String to look for in mount entry to determine if it is a user created fuse mount
  private static final String FUSE_MOUNT_ENTRY = "fuse";

  protected record MountEntry(String bucketName, String mountPath, String mountDetails) {}

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
   * @param disableCache Whether to disable file metadata caching for the mount
   * @param readOnly Whether override the default mount behavior and mount as read-only if true and
   *     read-write if false
   * @return number of resources returning a nonzero exit code from their mount process
   */
  public int mountResources(boolean disableCache, @Nullable Boolean readOnly) {
    List<Resource> resources = Context.requireWorkspace().listResources();
    Map<UUID, Path> folderPaths = getFolderIdToFolderPathMap();

    return resources.stream()
        .filter(this::isMountableResource)
        .mapToInt(
            r -> {
              Path mountPath = getResourceMountPath(r, folderPaths);
              return mountResourceWorker(r, mountPath, disableCache, readOnly, false);
            })
        .sum();
  }

  /**
   * Mounts a single resource
   *
   * @param resourceName The name of the resource to mount
   * @param disableCache Whether to disable file metadata caching for the mount
   * @param readOnly Whether override the default mount behavior and mount as read-only if true and
   *     read-write if false
   */
  public void mountResource(String resourceName, boolean disableCache, @Nullable Boolean readOnly) {
    // Fetch resource with provided name, throw not found exception if not found
    Resource resource = Context.requireWorkspace().getResource(resourceName);
    // Validate that the resource is a mountable resource
    if (!isMountableResource(resource)) {
      throw new UserActionableException(
          String.format("%s is not a bucket or a referenced bucket folder.", resourceName));
    }
    // Get the path to mount the resource to
    Map<UUID, Path> folderPaths = getFolderIdToFolderPathMap();
    Path mountPath = getResourceMountPath(resource, folderPaths);
    // Mount the resource
    mountResourceWorker(resource, mountPath, disableCache, readOnly, true);
  }

  /**
   * Builds and runs a mount handler a given resource, mount path, and mount options.
   *
   * @param resource The resource to mount
   * @param mountPath The path to mount the resource to
   * @param disableCache Whether to disable file metadata caching for the mount
   * @param readOnly Whether override the default mount behavior and mount as read-only if true and
   *     read-write if false
   * @param throwOnError Whether to throw an exception if the mount fails
   * @return the exit code of the mount subprocess
   */
  public int mountResourceWorker(
      Resource resource,
      Path mountPath,
      boolean disableCache,
      @Nullable Boolean readOnly,
      Boolean throwOnError) {

    // Clean up the mount path directory and error state directories if they exist
    BaseMountHandler.cleanupMountPath(mountPath);
    // Create the mount directory if it doesn't exist
    FileUtils.createDirectories(mountPath);
    // Build and run the mount handler
    boolean mountReadOnly = Objects.requireNonNullElseGet(readOnly, () -> !createdByUser(resource));
    BaseMountHandler handler = getMountHandler(resource, mountPath, disableCache, mountReadOnly);
    int exitCode = handler.mount();
    // Throw an exception if the mount failed
    if (throwOnError && exitCode != 0) {
      throw new UserActionableException(
          String.format("Failed to mount resource %s.", resource.getName()));
    }
    return exitCode;
  }

  /** Unmount all mountable resources for a given workspace */
  public void unmountResources() {
    listMountEntries()
        .map(this::getResourceMountEntry)
        .filter(Objects::nonNull)
        .forEach(mountEntry -> BaseMountHandler.unmount(Path.of(mountEntry.mountPath)));
    // Delete empty directories in WORKSPACE_DIR, throw an error there are any nonempty directories
    FileUtils.deleteEmptyDirectories(getWorkspaceDir());
  }

  /**
   * Unmount a single resource with the provided name
   *
   * @param silent if true, do not throw an exception if the resource is not mounted.
   *     <p>We unmount silently if we are calling this method as a part of cleaning the mount entry
   *     before mounting. If the user directly unmounts a resource, we do want to throw an
   *     exception.
   */
  public void unmountResource(String resourceName, boolean silent) {
    // Fetch resource with provided name, throw exception if not found
    Resource resource = Context.requireWorkspace().getResource(resourceName);
    // Validate that the resource is a mountable resource
    if (!isMountableResource(resource)) {
      throw new UserActionableException(
          String.format("%s is not a bucket or a referenced bucket folder.", resourceName));
    }

    String bucketName = getBucketNameFromResource(resource);

    // Get the mount entry for the resource
    List<MountEntry> mountEntries =
        listMountEntries()
            .map(this::getResourceMountEntry)
            .filter(Objects::nonNull)
            .filter(entry -> entry.bucketName.equals(bucketName))
            .toList();
    // Validate that the resource is mounted in one or more locations.
    if (!silent && mountEntries.isEmpty()) {
      throw new UserActionableException(String.format("Resource %s is not mounted.", resourceName));
    }
    // Unmount the resource
    mountEntries.forEach(
        mountEntry -> {
          Path path = Path.of(mountEntry.mountPath);
          BaseMountHandler.unmount(path);
          BaseMountHandler.cleanupMountPath(path);
        });
  }

  public void unmountResource(String resourceName) {
    unmountResource(resourceName, /*silent=*/ false);
  }

  /**
   * Helper method to list mount entries from the `mount` command
   *
   * @return the command output as a stream of lines
   */
  private Stream<String> listMountEntries() {
    List<String> command = new ArrayList<>(Collections.singleton(LIST_MOUNT_ENTRIES_COMMAND));
    LocalProcessLauncher localProcessLauncher = LocalProcessLauncher.create();
    localProcessLauncher.launchProcess(command, null, null);

    int exitCode = localProcessLauncher.waitForTerminate();
    if (exitCode != 0) {
      throw new SystemException("Failed to query mounted resources.");
    }
    return new BufferedReader(new InputStreamReader(localProcessLauncher.getInputStream())).lines();
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
   * reference to a file containing resource. E.g. GCS bucket, GCS object that points to a bucket
   * folder.
   */
  @VisibleForTesting
  public boolean isMountableResource(Resource r) {
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
          && mountEntry.mountDetails.contains(FUSE_MOUNT_ENTRY)) {
        return mountEntry;
      }
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
  public BaseMountHandler getMountHandler(
      Resource r, Path mountPoint, boolean disableCache, boolean readOnly) {
    return switch (r.getResourceType()) {
      case GCS_BUCKET -> new GcsFuseMountHandler((GcsBucket) r, mountPoint, disableCache, readOnly);
      case GCS_OBJECT -> new GcsFuseMountHandler((GcsObject) r, mountPoint, disableCache, readOnly);
      default -> throw new SystemException("Unsupported resource type: " + r.getResourceType());
    };
  }

  /**
   * Helper method to determine if a resource is a controlled GCS bucket created by the current
   * user.
   *
   * @param r resource to check
   * @return true if the resource is a controlled GCS bucket created by the current user, false
   *     otherwise.
   */
  private boolean createdByUser(Resource r) {
    return r.getResourceType().equals(Resource.Type.GCS_BUCKET)
        && r.getStewardshipType().equals(StewardshipType.CONTROLLED)
        && r.getCreatedBy().equals(Context.requireUser().getEmail());
  }

  /**
   * Helper method to get the bucket name from a resource.
   *
   * @param r resource to get the bucket name from
   * @return bucket name, empty if resource is not a GCS bucket or object
   */
  @VisibleForTesting
  public String getBucketNameFromResource(Resource r) {
    return r.getResourceType().equals(Resource.Type.GCS_BUCKET)
        ? ((GcsBucket) r).getBucketName()
        : r.getResourceType().equals(Resource.Type.GCS_OBJECT)
            ? ((GcsObject) r).getBucketName()
            : "";
  }
}
