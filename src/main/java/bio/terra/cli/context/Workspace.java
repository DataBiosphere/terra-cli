package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POJO class that represents a workspace. This class is serialized to disk as part of the global
 * context. It is also used as the user-facing JSON output for commands that return a workspace.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Workspace {
  private static final Logger logger = LoggerFactory.getLogger(Workspace.class);

  // properties of the workspace
  public final UUID id;
  public final String name; // not unique
  public final String description;
  public final String googleProjectId;

  // name of the server where this workspace exists
  public final String serverName;

  // email of the user that loaded the workspace to this machine
  public final String terraUserEmail;

  // list of resources (controlled & referenced)
  private List<Resource> resources;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  private Workspace(
      @JsonProperty("id") UUID id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("googleProjectId") String googleProjectId,
      @JsonProperty("serverName") String serverName,
      @JsonProperty("terraUserEmail") String terraUserEmail,
      @JsonProperty("resources") List<Resource> resources) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.googleProjectId = googleProjectId;
    this.serverName = serverName;
    this.terraUserEmail = terraUserEmail;
    this.resources = resources;
  }

  /**
   * Create a new workspace and set it as the current workspace.
   *
   * @param name optional display name
   * @param description optional description
   */
  public static Workspace create(String name, String description) {
    // call WSM to create the workspace object and backing Google context
    WorkspaceDescription createdWorkspace =
        new WorkspaceManagerService().createWorkspace(name, description);
    logger.info("Created workspace: {}", createdWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = fromWSMObject(createdWorkspace);

    // fetch the pet SA credentials for the user + this workspace
    GlobalContext globalContext = GlobalContext.get();
    globalContext.requireCurrentTerraUser().fetchPetSaCredentials();

    // update the global context with the current workspace
    GlobalContext.get().setCurrentWorkspace(workspace);
    return workspace;
  }

  /**
   * Load an existing workspace and set it as the current workspace.
   *
   * @param id workspace id
   */
  public static Workspace load(UUID id) {
    // call WSM to fetch the existing workspace object and backing Google context
    WorkspaceDescription loadedWorkspace = new WorkspaceManagerService().getWorkspace(id);
    logger.info("Loaded workspace: {}", loadedWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = fromWSMObject(loadedWorkspace);

    // fetch the pet SA credentials for the user + this workspace
    GlobalContext globalContext = GlobalContext.get();
    globalContext.requireCurrentTerraUser().fetchPetSaCredentials();

    // update the global context with the current workspace
    GlobalContext.get().setCurrentWorkspace(workspace);

    // fetch the list of resources in this workspace
    Resource.listAndSync();

    return workspace;
  }

  /**
   * Update the mutable properties of the current workspace.
   *
   * @param name optional display name
   * @param description optional description
   * @throws UserActionableException if there is no current workspace
   */
  public Workspace update(String name, String description) {
    // check that there is a current workspace
    GlobalContext globalContext = GlobalContext.get();
    globalContext.requireCurrentWorkspace();

    // call WSM to update the existing workspace object
    WorkspaceDescription updatedWorkspace =
        new WorkspaceManagerService().updateWorkspace(id, name, description);
    logger.info("Updated workspace: {}", updatedWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = fromWSMObject(updatedWorkspace);

    // update the global context with the current workspace
    GlobalContext.get().setCurrentWorkspace(workspace);
    return workspace;
  }

  /** Delete the current workspace. */
  public void delete() {
    // call WSM to delete the existing workspace object
    new WorkspaceManagerService().deleteWorkspace(id);
    logger.info("Deleted workspace: {}", this);

    // delete the pet SA credentials for the user
    GlobalContext globalContext = GlobalContext.get();
    globalContext.requireCurrentTerraUser().deletePetSaCredentials();

    // unset the workspace in the current context
    globalContext.unsetCurrentWorkspace();
  }

  /**
   * List all workspaces that a user has read access to.
   *
   * @param offset the offset to use when listing workspaces (zero to start from the beginning)
   * @param limit the maximum number of workspaces to return
   * @return list of workspaces
   */
  public static List<Workspace> list(int offset, int limit) {
    // fetch the list of workspaces from WSM
    List<WorkspaceDescription> listedWorkspaces =
        new WorkspaceManagerService().listWorkspaces(offset, limit).getWorkspaces();

    // convert the WSM objects to CLI objects
    List<Workspace> workspaces = new ArrayList<>();
    listedWorkspaces.forEach(wsmObject -> workspaces.add(fromWSMObject(wsmObject)));
    return workspaces;
  }

  /**
   * Helper method to convert a WSM client library WorkspaceDescription object to a CLI Workspace
   * object.
   */
  private static Workspace fromWSMObject(WorkspaceDescription wsmObject) {
    GlobalContext globalContext = GlobalContext.get();
    Workspace workspace =
        new Workspace(
            wsmObject.getId(),
            wsmObject.getDisplayName() == null ? "" : wsmObject.getDisplayName(),
            wsmObject.getDescription() == null ? "" : wsmObject.getDescription(),
            wsmObject.getGcpContext() == null ? null : wsmObject.getGcpContext().getProjectId(),
            globalContext.server.name,
            globalContext.requireCurrentTerraUser().getEmail(),
            new ArrayList<>());
    return workspace;
  }

  /** Print out a workspace object in text format. */
  public void printText() {
    PrintStream OUT = Printer.getOut();
    OUT.println("Terra workspace id: " + id);
    OUT.println("Display name: " + name);
    OUT.println("Description: " + description);
    OUT.println("Google project: " + googleProjectId);
    OUT.println(
        "Cloud console: https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
  }

  /** Getter for the resources list. Returns an immutable copy. */
  @JsonIgnore
  public List<Resource> getResources() {
    return Collections.unmodifiableList(resources);
  }

  /** Setter for the resources list. */
  @JsonIgnore
  public void setResources(List<Resource> resources) {
    this.resources = resources;
    GlobalContext.get().writeToFile();
  }

  /**
   * Get a resource by name.
   *
   * @throws UserActionableException if there is no resource with that name
   */
  @JsonIgnore
  public Resource getResource(String name) {
    Optional<Resource> resourceOpt =
        GlobalContext.get().requireCurrentWorkspace().getResources().stream()
            .filter(resource -> resource.name.equals(name))
            .findFirst();
    if (resourceOpt.isEmpty()) {
      throw new UserActionableException("Resource not found: " + name);
    }
    return resourceOpt.get();
  }

  /** Add a new resource to the list. */
  @JsonIgnore
  public void addResource(Resource resource) {
    resources.add(resource);
    GlobalContext.get().writeToFile();
  }

  /** Remove a resource from the list. */
  @JsonIgnore
  public void removeResource(String name) {
    resources.removeIf(resource -> resource.name.equals(name));
    GlobalContext.get().writeToFile();
  }
}
