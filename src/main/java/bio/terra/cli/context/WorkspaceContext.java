package bio.terra.cli.context;

import bio.terra.cli.context.utils.FileUtils;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final Path WORKSPACE_CONTEXT_DIR = Paths.get("", ".terra");
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
    WorkspaceContext workspaceContext = null;
    try {
      workspaceContext =
          FileUtils.readFileIntoJavaObject(
              resolveWorkspaceContextFile().toFile(), WorkspaceContext.class);
    } catch (IOException ioEx) {
      logger.error("Workspace context file not found.", ioEx);
    }

    // if the workspace context file does not exist, return an object populated with default values
    if (workspaceContext == null) {
      workspaceContext = new WorkspaceContext();
    }

    return workspaceContext;
  }

  /**
   * Write an instance of this class to a JSON-formatted file in the workspace context directory.
   */
  private void writeToFile() {
    try {
      FileUtils.writeJavaObjectToFile(resolveWorkspaceContextFile().toFile(), this);
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
    logger.debug(
        "Updating workspace from {} to {}.",
        getWorkspaceId(),
        terraWorkspaceModel == null ? null : terraWorkspaceModel.getId());
    this.terraWorkspaceModel = terraWorkspaceModel;

    writeToFile();
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
    return terraWorkspaceModel == null || terraWorkspaceModel.getGoogleContext() == null
        ? null
        : terraWorkspaceModel.getGoogleContext().getProjectId();
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
      throw new RuntimeException("There is no Terra workspace mounted to the current directory.");
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
   * #listControlledResources()} that filters for just the controlled ones.
   *
   * @return list of controlled resources in the workspace
   */
  public List<CloudResource> listControlledResources() {
    return cloudResources.values().stream()
        .filter(dataReference -> dataReference.isControlled)
        .collect(Collectors.toList());
  }

  // ====================================================
  // Directory and file names
  //   - current working directory: .
  //   - persisted workspace context file: ./terra-cli/workspace_context.json
  //   - sub-directories for tools (e.g. ./nextflow) are defined in the SupportedToolHelper
  // sub-classes

  /**
   * Getter for the file where the workspace context is persisted.
   *
   * @return path to the workspace context file
   */
  public static Path resolveWorkspaceContextFile() {
    return WORKSPACE_CONTEXT_DIR.resolve(WORKSPACE_CONTEXT_FILENAME);
  }
}
