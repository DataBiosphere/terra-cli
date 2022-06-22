package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.PDResource;
import bio.terra.cli.serialization.persisted.PDWorkspace;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.cloudresourcemanager.CloudResourceManagerCow;
import bio.terra.workspace.model.CloneWorkspaceResult;
import bio.terra.workspace.model.ClonedWorkspace;
import bio.terra.workspace.model.Properties;
import bio.terra.workspace.model.Property;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.WorkspaceDescription;
import com.google.api.services.cloudresourcemanager.v3.model.Binding;
import com.google.api.services.cloudresourcemanager.v3.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.v3.model.Policy;
import com.google.api.services.cloudresourcemanager.v3.model.SetIamPolicyRequest;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  private UUID uuid;
  private String userFacingId;
  private String name; // not unique
  private String description;
  private String googleProjectId;
  private Properties properties;

  // name of the server where this workspace exists
  private String serverName;

  // email of the user that loaded the workspace to this machine
  private String userEmail;

  // list of resources (controlled & referenced)
  private List<Resource> resources;

  /** Build an instance of this class from the WSM client library WorkspaceDescription object. */
  private Workspace(WorkspaceDescription wsmObject) {
    this.uuid = wsmObject.getId();
    this.userFacingId = wsmObject.getUserFacingId();
    this.name = wsmObject.getDisplayName() == null ? "" : wsmObject.getDisplayName();
    this.description = wsmObject.getDescription() == null ? "" : wsmObject.getDescription();
    this.googleProjectId =
        wsmObject.getGcpContext() == null ? null : wsmObject.getGcpContext().getProjectId();
    this.properties = wsmObject.getProperties();
    this.serverName = Context.getServer().getName();
    this.userEmail = Context.requireUser().getEmail();
    this.resources = new ArrayList<>();
  }

  /** Build an instance of this class from the serialized format on disk. */
  public Workspace(PDWorkspace configFromDisk) {
    this.uuid = configFromDisk.uuid;
    this.userFacingId = configFromDisk.userFacingId;
    this.name = configFromDisk.name;
    this.description = configFromDisk.description;
    this.googleProjectId = configFromDisk.googleProjectId;
    this.properties = configFromDisk.properties;
    this.serverName = configFromDisk.serverName;
    this.userEmail = configFromDisk.userEmail;
    this.resources =
        configFromDisk.resources.stream()
            .map(PDResource::deserializeToInternal)
            .collect(Collectors.toList());
  }

  /** Create a new workspace and set it as the current workspace. */
  public static Workspace create(
      String userFacingId, String name, String description, Map<String, String> properties) {
    // call WSM to create the workspace object and backing Google context
    WorkspaceDescription createdWorkspace =
        WorkspaceManagerService.fromContext()
            .createWorkspace(userFacingId, name, description, stringMapToProperties(properties));
    logger.info("Created workspace: {}", createdWorkspace);

    // convert the WSM object to a CLI object
    Workspace workspace = new Workspace(createdWorkspace);

    // update the global context with the current workspace
    Context.setWorkspace(workspace);

    // fetch the pet SA email for the user + this workspace
    // do this here so we have them stored locally before the user tries to run an app in the
    // workspace. this is so we pay the cost of a SAM round-trip ahead of time, instead of slowing
    // down an app call
    Context.requireUser().fetchPetSaEmail();

    return workspace;
  }

  /** Load an existing workspace and set it as the current workspace. */
  public static Workspace load(String userFacingId) {
    Workspace workspace = Workspace.get(userFacingId);

    // update the global context with the current workspace
    Context.setWorkspace(workspace);

    // fetch the pet SA email for the user + this workspace
    // do this here so we have them stored locally before the user tries to run an app in the
    // workspace. this is so we pay the cost of a SAM round-trip ahead of time, instead of slowing
    // down an app call
    Context.requireUser().fetchPetSaEmail();

    return workspace;
  }

  /** Fetch an existing workspace by uuid, with resources populated */
  public static Workspace get(UUID id) {
    // call WSM to fetch the existing workspace object and backing Google context
    WorkspaceDescription loadedWorkspace = WorkspaceManagerService.fromContext().getWorkspace(id);
    logger.info("Loaded workspace: {}", loadedWorkspace);
    return convertWorkspaceDescriptionToWorkspace(loadedWorkspace);
  }

  /** Fetch an existing workspace by userFacingId, with resources populated */
  public static Workspace get(String userFacingId) {
    // call WSM to fetch the existing workspace object and backing Google context
    WorkspaceDescription loadedWorkspace =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(userFacingId);
    logger.info("Loaded workspace: {}", loadedWorkspace);
    return convertWorkspaceDescriptionToWorkspace(loadedWorkspace);
  }

  /** Convert what's returned from WSM API to CLI object. */
  private static Workspace convertWorkspaceDescriptionToWorkspace(
      WorkspaceDescription workspaceDescription) {
    Workspace workspace = new Workspace(workspaceDescription);
    workspace.populateResources();
    return workspace;
  }

  /**
   * Update the mutable properties of the current workspace.
   *
   * @param userFacingId optional user-facing ID
   * @param name optional display name
   * @param description optional description
   * @throws UserActionableException if there is no current workspace
   */
  public Workspace update(
      @Nullable String userFacingId, @Nullable String name, @Nullable String description) {
    // call WSM to update the existing workspace object
    WorkspaceDescription updatedWorkspace =
        WorkspaceManagerService.fromContext()
            .updateWorkspace(uuid, userFacingId, name, description);
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
    WorkspaceManagerService.fromContext().deleteWorkspace(uuid);
    logger.info("Deleted workspace: {}", this);

    // delete the pet SA email for the user
    Context.requireUser().deletePetSaEmail();

    // unset the workspace in the current context
    Context.setWorkspace(null);
  }

  /**
   * Enable the current user and their pet to impersonate their pet SA in this workspace.
   *
   * @return Email identifier of the pet SA the current user can now actAs.
   */
  public void enablePet() {
    WorkspaceManagerService.fromContext().enablePet(uuid);
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
            .enumerateAllResources(uuid, Context.getConfig().getResourcesCacheSize());
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

  /**
   * Clone the current workspace into a new one
   *
   * @param userFacingId - user-facing ID of the new workspace
   * @param name - name of the new workspace
   * @param description - description of the new workspace
   * @param properties - property of the new workspace
   * @return - ClonedWorkspace structure with details on each resource
   */
  public ClonedWorkspace clone(
      String userFacingId,
      @Nullable String name,
      @Nullable String description,
      @Nullable Map<String, String> properties) {
    CloneWorkspaceResult result =
        WorkspaceManagerService.fromContext()
            .cloneWorkspace(
                uuid, userFacingId, name, description, stringMapToProperties(properties));
    return result.getWorkspace();
  }

  /**
   * Grant break-glass access to a user of this workspace. The Editor and Project IAM Admin roles
   * are granted to the user's proxy group.
   *
   * @param granteeEmail email of the workspace user requesting break-glass access
   * @param userProjectsAdminCredentials credentials for a SA that has permission to set IAM policy
   *     on workspace projects in this WSM deployment (e.g. WSM application SA)
   * @return the proxy group email of the workspace user that was granted break-glass access
   */
  public String grantBreakGlass(
      String granteeEmail, ServiceAccountCredentials userProjectsAdminCredentials) {
    // fetch the user's proxy group email from SAM
    String granteeProxyGroupEmail = SamService.fromContext().getProxyGroupEmail(granteeEmail);
    logger.debug("granteeProxyGroupEmail: {}", granteeProxyGroupEmail);

    // grant the Editor role to the user's proxy group email on the workspace project
    CloudResourceManagerCow resourceManagerCow =
        CrlUtils.createCloudResourceManagerCow(userProjectsAdminCredentials);
    try {
      Policy policy =
          resourceManagerCow
              .projects()
              .getIamPolicy(googleProjectId, new GetIamPolicyRequest())
              .execute();
      List<Binding> updatedBindings =
          Optional.ofNullable(policy.getBindings()).orElse(new ArrayList<>());
      updatedBindings.add(
          new Binding()
              .setRole("roles/editor")
              .setMembers(ImmutableList.of("group:" + granteeProxyGroupEmail)));
      updatedBindings.add(
          new Binding()
              .setRole("roles/resourcemanager.projectIamAdmin")
              .setMembers(ImmutableList.of("group:" + granteeProxyGroupEmail)));
      policy.setBindings(updatedBindings);
      resourceManagerCow
          .projects()
          .setIamPolicy(googleProjectId, new SetIamPolicyRequest().setPolicy(policy))
          .execute();
    } catch (IOException ioEx) {
      throw new SystemException(
          "Error granting the Editor and Project IAM Admin roles to the user's proxy group.", ioEx);
    }

    return granteeProxyGroupEmail;
  }

  private static Properties stringMapToProperties(Map<String, String> map) {
    Properties properties = new Properties();
    if (map == null) {
      return properties;
    }
    for (Map.Entry<String, String> entry : map.entrySet()) {
      Property property = new Property().key(entry.getKey()).value(entry.getValue());
      properties.add(property);
    }
    return properties;
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getUserFacingId() {
    return userFacingId;
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

  public Properties getProperties() {
    return properties;
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
}
