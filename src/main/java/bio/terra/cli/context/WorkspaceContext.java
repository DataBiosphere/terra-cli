package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents an instance of the Terra CLI workspace context. This is intended
 * primarily for project and resource-related context values that will be particular to a single
 * workspace.
 */
public class WorkspaceContext {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceContext.class);

  // workspace description object returned by WSM
  public WorkspaceDescription terraWorkspaceModel;

  // map of cloud resources for this workspace (name -> object)
  public Map<String, CloudResource> cloudResources;

  // file paths related to persisting the workspace context on disk
  private static final String WORKSPACE_CONTEXT_DIRNAME = ".terra";
  private static final String WORKSPACE_CONTEXT_FILENAME = "workspace-context.json";

  private WorkspaceContext() {
    this.terraWorkspaceModel = null;
    this.cloudResources = new HashMap<>();
  }

  // ====================================================
  // Persisting on disk

  /**
   * Read in an instance of this class from a JSON-formatted file in the current directory. If there
   * is no existing file, this method returns an object populated with default values.
   *
   * @return an instance of this class
   */
  public static WorkspaceContext readFromFile() {
    // try to read in an instance of the workspace context file
    try {
      return FileUtils.readFileIntoJavaObject(
          getWorkspaceContextFile().toFile(), WorkspaceContext.class);
    } catch (IOException ioEx) {
      logger.debug("Workspace context file not found or error reading it.", ioEx);
    }

    // if the workspace context file does not exist or there is an error reading it, return an
    // object populated with default values
    return new WorkspaceContext();
  }

  /**
   * Write an instance of this class to a JSON-formatted file in the workspace context directory.
   */
  private void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(getWorkspaceContextFile().toFile(), this);
    } catch (IOException ioEx) {
      logger.error("Error persisting workspace context.", ioEx);
    }
  }

  // ====================================================
  // Workspace

  /**
   * Setter for the current Terra workspace. Persists on disk.
   *
   * @param terraWorkspaceModel the workspace description object
   */
  public void updateWorkspace(WorkspaceDescription terraWorkspaceModel) {
    logger.info("Updating workspace from {} to {}.", getWorkspaceId(), terraWorkspaceModel.getId());
    this.terraWorkspaceModel = terraWorkspaceModel;

    writeToFile();
  }

  /** Delete the current Terra workspace context. Persists on disk. */
  public void deleteWorkspace() {
    logger.info("Deleting workspace {}", getWorkspaceId());
    this.terraWorkspaceModel = null;
    this.cloudResources = null;

    writeToFile();
  }

  /**
   * Getter for the Terra workspace display name. Returns empty Optional if the display name is not
   * defined.
   *
   * @return the Terra workspace display name
   */
  @JsonIgnore
  public Optional<String> getWorkspaceDisplayName() {
    return Optional.ofNullable(terraWorkspaceModel).map(WorkspaceDescription::getDisplayName);
  }

  /**
   * Getter for the Terra workspace description. Returns empty Optional if the description is not
   * defined.
   *
   * @return the Terra workspace description
   */
  @JsonIgnore
  public Optional<String> getWorkspaceDescription() {
    return Optional.ofNullable(terraWorkspaceModel).map(WorkspaceDescription::getDescription);
  }

  /**
   * Getter for the Terra workspace id.
   *
   * @return the Terra workspace id
   */
  @JsonIgnore
  public UUID getWorkspaceId() {
    return terraWorkspaceModel == null ? null : terraWorkspaceModel.getId();
  }

  /**
   * Getter for the Google project backing the current Terra workspace.
   *
   * @return the Google project id
   */
  @JsonIgnore
  public String getGoogleProject() {
    return terraWorkspaceModel == null || terraWorkspaceModel.getGcpContext() == null
        ? null
        : terraWorkspaceModel.getGcpContext().getProjectId();
  }

  /** Utility method to test whether a workspace is set in the current context. */
  @JsonIgnore
  public boolean isEmpty() {
    return terraWorkspaceModel == null;
  }

  /**
   * Utility method that throws an exception if there is no workspace set in the current context.
   */
  public void requireCurrentWorkspace() {
    if (isEmpty()) {
      throw new UserActionableException(
          "There is no Terra workspace mounted to the current directory.");
    }
  }

  // ====================================================
  // Cloud resources

  /**
   * Lookup a cloud resource by its name. Names are unique within a workspace.
   *
   * @param name cloud resource name
   * @return cloud resource object
   */
  public CloudResource getCloudResource(String name) {
    return cloudResources.get(name);
  }

  /**
   * Add a cloud resource to the list for this workspace. Persists on disk.
   *
   * @param cloudResource cloud resource to add
   */
  public void addCloudResource(CloudResource cloudResource) {
    cloudResources.put(cloudResource.name, cloudResource);

    writeToFile();
  }

  /**
   * Remove a cloud resource from the list of cloud resources for this workspace. Persists on disk.
   *
   * @param name cloud resource name
   */
  public void removeCloudResource(String name) {
    cloudResources.remove(name);

    writeToFile();
  }

  /**
   * List all cloud resources in the workspace.
   *
   * @return list of cloud resources in the workspace
   */
  public List<CloudResource> listCloudResources() {
    return new ArrayList<>(cloudResources.values());
  }

  /**
   * List all controlled cloud resources for the workspace. This is a utility wrapper around {@link
   * #listCloudResources()} that filters for just the controlled ones.
   *
   * @return list of controlled resources in the workspace
   */
  public List<CloudResource> listControlledResources() {
    return cloudResources.values().stream()
        .filter(cloudResource -> cloudResource.isControlled)
        .collect(Collectors.toList());
  }

  /**
   * List all data references for this workspace. This is a utility wrapper around {@link
   * #listCloudResources()} that filters for just the data references.
   *
   * @return list of data references in the workspace
   */
  public List<CloudResource> listDataReferences() {
    return cloudResources.values().stream()
        .filter(cloudResource -> cloudResource.type.isDataReference)
        .collect(Collectors.toList());
  }

  // ====================================================
  // Resolving file and directory paths

  /**
   * Get the workspace directory. (i.e. the parent of the .terra directory)
   *
   * @return the absolute path to the workspace directory
   */
  @SuppressFBWarnings(
      value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
      justification =
          "An NPE would only happen here if there was an error getting the workspace context file, "
              + "and an exception would be thrown in that method instead.")
  @JsonIgnore
  public static Path getWorkspaceDir() {
    return getWorkspaceContextFile().getParent().getParent();
  }

  /**
   * Get the workspace context file.
   *
   * <p>This method first searches for an existing workspace context file in the current directory
   * hierarchy. If it finds one, then it returns that file.
   *
   * <p>If it does not find one, then it returns the file where a new workspace context can be
   * written. This file will be relative to the current directory (i.e. current directory =
   * workspace top-level directory)
   *
   * @return absolute path to the workspace context file
   */
  @JsonIgnore
  private static Path getWorkspaceContextFile() {
    Path currentDir = Path.of("").toAbsolutePath();
    try {
      return getExistingWorkspaceContextFile(currentDir);
    } catch (FileNotFoundException fnfEx) {
      return currentDir.resolve(WORKSPACE_CONTEXT_DIRNAME).resolve(WORKSPACE_CONTEXT_FILENAME);
    }
  }

  /**
   * Get the existing workspace context file in the current directory hierarchy.
   *
   * <p>For each directory, it checks for the existence of a workspace context file (i.e.
   * ./.terra/workspace-context.json).
   *
   * <p>-If it finds one, then it returns that file.
   *
   * <p>-Otherwise, it recursively checks the parent directory, until it hits the root directory.
   *
   * <p>-Once it hits the root directory, and still doesn't find a workspace context file, it throws
   * an exception.
   *
   * @param currentDir the directory to search
   * @return absolute path to the existing workspace context file
   * @throws FileNotFoundException if no existing workspace context file is found
   */
  @JsonIgnore
  private static Path getExistingWorkspaceContextFile(Path currentDir)
      throws FileNotFoundException {
    // get the workspace context sub-directory relative to the current directory and check if it
    // exists
    Path workspaceContextDir = currentDir.resolve(WORKSPACE_CONTEXT_DIRNAME);
    File workspaceContextDirHandle = workspaceContextDir.toFile();
    if (workspaceContextDirHandle.exists() && workspaceContextDirHandle.isDirectory()) {

      // get the workspace context file relative to the sub-directory and check if it exists
      Path workspaceContextFile = workspaceContextDir.resolve(WORKSPACE_CONTEXT_FILENAME);
      if (workspaceContextFile.toFile().exists() && workspaceContextFile.toFile().isFile()) {
        return workspaceContextFile.toAbsolutePath();
      }
    }

    // if we've reached the root directory, then no existing workspace context file is found
    Path parentDir = currentDir.toAbsolutePath().getParent();
    if (currentDir.getNameCount() == 0 || parentDir == null) {
      throw new FileNotFoundException(
          "No workspace context file found in the current directory hierarchy");
    }

    // recursively check the parent directory
    return getExistingWorkspaceContextFile(parentDir);
  }
}
