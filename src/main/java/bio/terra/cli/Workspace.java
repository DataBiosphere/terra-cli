package bio.terra.cli;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.disk.DiskWorkspace;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.WorkspaceDescription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Workspace {
  private static final Logger logger = LoggerFactory.getLogger(Workspace.class);

  private UUID id;
  private String name; // not unique
  private String description;
  private String googleProjectId;

  // name of the server where this workspace exists
  private String serverName;

  // email of the user that loaded the workspace to this machine
  private String userEmail;

  // list of resources (controlled & referenced)
  private List<Resource> resources;

  /** Build an instance of this class from the WSM client library WorkspaceDescription object. */
  private Workspace(WorkspaceDescription wsmObject) {
    this.id = wsmObject.getId();
    this.name = wsmObject.getDisplayName() == null ? "" : wsmObject.getDisplayName();
    this.description = wsmObject.getDescription() == null ? "" : wsmObject.getDescription();
    this.googleProjectId =
        wsmObject.getGcpContext() == null ? null : wsmObject.getGcpContext().getProjectId();
    this.serverName = Context.getServer().getName();
    this.userEmail = Context.requireUser().getEmail();
    this.resources = new ArrayList<>();
  }

  /** Build an instance of this class from the serialized format on disk. */
  public Workspace(DiskWorkspace configFromDisk) {
    this.id = configFromDisk.id;
    this.name = configFromDisk.name;
    this.description = configFromDisk.description;
    this.googleProjectId = configFromDisk.googleProjectId;
    this.serverName = configFromDisk.serverName;
    this.userEmail = configFromDisk.userEmail;
    this.resources =
        configFromDisk.resources.stream()
            .map(diskResource -> diskResource.resourceType.getBuilder(diskResource).build())
            .collect(Collectors.toList());
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
    Workspace workspace = new Workspace(createdWorkspace);

    // fetch the pet SA credentials for the user + this workspace
    Context.requireUser().fetchPetSaCredentials();

    // update the global context with the current workspace
    Context.setWorkspace(workspace);
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
    Workspace workspace = new Workspace(loadedWorkspace);

    // update the global context with the current workspace
    Context.setWorkspace(workspace);

    // fetch the pet SA credentials for the user + this workspace
    Context.requireUser().fetchPetSaCredentials();

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
    // call WSM to update the existing workspace object
    WorkspaceDescription updatedWorkspace =
        new WorkspaceManagerService().updateWorkspace(id, name, description);
    logger.info("Updated workspace: {}", updatedWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = new Workspace(updatedWorkspace);

    // update the global context with the current workspace
    Context.setWorkspace(workspace);
    return workspace;
  }

  /** Delete the current workspace. */
  public void delete() {
    // call WSM to delete the existing workspace object
    new WorkspaceManagerService().deleteWorkspace(id);
    logger.info("Deleted workspace: {}", this);

    // delete the pet SA credentials for the user
    Context.requireUser().deletePetSaCredentials();

    // unset the workspace in the current context
    Context.setWorkspace(null);
  }

  /**
   * List all workspaces that the current user has read access to.
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
    listedWorkspaces.forEach(wsmObject -> workspaces.add(new Workspace(wsmObject)));
    return workspaces;
  }

  /**
   * Get a resource by name.
   *
   * @throws UserActionableException if there is no resource with that name
   */
  public Resource getResource(String name) {
    Optional<Resource> resourceOpt =
        resources.stream().filter(resource -> resource.name.equals(name)).findFirst();
    if (resourceOpt.isEmpty()) {
      throw new UserActionableException("Resource not found: " + name);
    }
    return resourceOpt.get();
  }

  // ====================================================
  // Property get/setters.
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getGoogleProjectId() {
    return googleProjectId;
  }

  public String getServerName() {
    return serverName;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public List<Resource> getResources() {
    return Collections.unmodifiableList(resources);
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
    Context.synchronizeToDisk();
  }
}
