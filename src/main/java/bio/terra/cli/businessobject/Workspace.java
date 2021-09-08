package bio.terra.cli.businessobject;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.PDResource;
import bio.terra.cli.serialization.persisted.PDWorkspace;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.WorkspaceDescription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a workspace. An instance of this class is part of the current context
 * or state.
 */
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

  // true if the workspace metadata was fetched. false when a user sets the workspace without being
  // logged in; in that case, we can't request the metadata from WSM without valid credentials.
  private boolean isLoaded;

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
    this.isLoaded = true;
  }

  /** Build an instance of this class from the serialized format on disk. */
  public Workspace(PDWorkspace configFromDisk) {
    this.id = configFromDisk.id;
    this.name = configFromDisk.name;
    this.description = configFromDisk.description;
    this.googleProjectId = configFromDisk.googleProjectId;
    this.serverName = configFromDisk.serverName;
    this.userEmail = configFromDisk.userEmail;
    this.resources =
        configFromDisk.resources.stream()
            .map(PDResource::deserializeToInternal)
            .collect(Collectors.toList());
    this.isLoaded = configFromDisk.isLoaded;
  }

  /**
   * Build an instance of this class from just the workspace id and server name. This is used when a
   * user sets the workspace without being logged in.
   */
  private Workspace(UUID id, String serverName) {
    this.id = id;
    this.serverName = serverName;
    this.resources = new ArrayList<>();
    this.isLoaded = false;
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
        WorkspaceManagerService.fromContext().createWorkspace(name, description);
    logger.info("Created workspace: {}", createdWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = new Workspace(createdWorkspace);

    // update the global context with the current workspace
    Context.setWorkspace(workspace);

    // fetch the pet SA credentials for the user + this workspace
    // do this here so we have them stored locally before the user tries to run an app in the
    // workspace. this is so we pay the cost of a SAM round-trip ahead of time, instead of slowing
    // down an app call
    Context.requireUser().fetchPetSaCredentials();

    return workspace;
  }

  /**
   * Load an existing workspace and set it as the current workspace.
   *
   * @param id workspace id
   */
  public static Workspace load(UUID id) {
    // a user can set the workspace without being logged in. in that case, we can't request the
    // metadata from WSM without valid credentials. so just save the workspace id for loading later.
    if (Context.getUser().isEmpty()) {
      Workspace loadedWorkspace = new Workspace(id, Context.getServer().getName());
      Context.setWorkspace(loadedWorkspace);
      return loadedWorkspace;
    }

    // call WSM to fetch the existing workspace object and backing Google context
    WorkspaceDescription loadedWorkspace = WorkspaceManagerService.fromContext().getWorkspace(id);
    logger.info("Loaded workspace: {}", loadedWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = new Workspace(loadedWorkspace);

    // fetch the list of resources in this workspace
    workspace.populateResources();

    // update the global context with the current workspace
    Context.setWorkspace(workspace);

    // fetch the pet SA credentials for the user + this workspace
    // do this here so we have them stored locally before the user tries to run an app in the
    // workspace. this is so we pay the cost of a SAM round-trip ahead of time, instead of slowing
    // down an app call
    Context.requireUser().fetchPetSaCredentials();

    return workspace;
  }

  /**
   * Update the mutable properties of the current workspace.
   *
   * @param name optional display name
   * @param description optional description
   * @throws UserActionableException if there is no current workspace
   */
  public Workspace update(@Nullable String name, @Nullable String description) {
    // call WSM to update the existing workspace object
    WorkspaceDescription updatedWorkspace =
        WorkspaceManagerService.fromContext().updateWorkspace(id, name, description);
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
    WorkspaceManagerService.fromContext().deleteWorkspace(id);
    logger.info("Deleted workspace: {}", this);

    // delete the pet SA credentials for the user
    Context.requireUser().deletePetSaCredentials();

    // unset the workspace in the current context
    Context.setWorkspace(null);
  }

  /**
   * Enable the current user to impersonate their pet SA in the current workspace.
   *
   * @return Email identifier of the pet SA the current user can now actAs.
   */
  public static String enablePet() {
    return WorkspaceManagerService.fromContext().enablePet(Context.requireWorkspace().getId());
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
        WorkspaceManagerService.fromContext().listWorkspaces(offset, limit).getWorkspaces();

    // convert the WSM objects to CLI objects
    return listedWorkspaces.stream().map(Workspace::new).collect(Collectors.toList());
  }

  /**
   * Get a resource by name and expect a specific type.
   *
   * @throws UserActionableException if the resource is not found or is the wrong type
   */
  public <T extends Resource> T getResourceOfType(String name, Resource.Type type) {
    Resource resource = getResource(name);
    if (!resource.getResourceType().equals(type)) {
      throw new UserActionableException("Invalid resource type: " + resource.getResourceType());
    }
    return (T) resource;
  }

  /**
   * Get a resource by name.
   *
   * @throws UserActionableException if there is no resource with that name
   */
  public Resource getResource(String name) {
    Optional<Resource> resourceOpt =
        resources.stream().filter(resource -> resource.name.equals(name)).findFirst();
    return resourceOpt.orElseThrow(
        () -> new UserActionableException("Resource not found: " + name));
  }

  /** Populate the list of resources for this workspace. Does not sync to disk. */
  private void populateResources() {
    List<ResourceDescription> wsmObjects =
        WorkspaceManagerService.fromContext()
            .enumerateAllResources(id, Context.getConfig().getResourcesCacheSize());
    List<Resource> resources =
        wsmObjects.stream().map(Resource::deserializeFromWsm).collect(Collectors.toList());

    this.resources = resources;
  }

  /**
   * Fetch the list of resources for the current workspace. Sync the cached list of resources to
   * disk.
   */
  public List<Resource> listResourcesAndSync() {
    populateResources();
    Context.synchronizeToDisk();
    return resources;
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

  public boolean getIsLoaded() {
    return isLoaded;
  }
}
