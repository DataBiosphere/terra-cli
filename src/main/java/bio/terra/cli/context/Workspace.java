package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POJO class that represents a workspace. This class is serialized to disk as part of the global
 * context. It is also used as the user-facing JSON output for commands that return a workspace.
 */
public class Workspace {
  private static final Logger logger = LoggerFactory.getLogger(Workspace.class);

  // properties of the workspace
  public final UUID id;
  public String name = ""; // not unique
  public String description = "";
  public final String googleProjectId;

  // list of resources (controlled & referenced)
  public List<ResourceDescription> resources;

  // name of the server where this workspace exists
  public final String serverName;

  // email of the user that loaded the workspace to this machine
  public final String terraUserEmail;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  private Workspace(
      @JsonProperty("id") UUID id,
      @JsonProperty("googleProjectId") String googleProjectId,
      @JsonProperty("serverName") String serverName,
      @JsonProperty("terraUserEmail") String terraUserEmail) {
    this.id = id;
    this.googleProjectId = googleProjectId;
    this.serverName = serverName;
    this.terraUserEmail = terraUserEmail;
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

    // update the global context with the current workspace
    GlobalContext.get().setCurrentWorkspace(workspace);
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

    // unset the workspace in the current context
    GlobalContext.get().unsetCurrentWorkspace();
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
            wsmObject.getGcpContext() == null ? null : wsmObject.getGcpContext().getProjectId(),
            globalContext.server.name,
            globalContext.terraUser.terraUserEmail);
    workspace.name = wsmObject.getDisplayName() == null ? "" : wsmObject.getDisplayName();
    workspace.description = wsmObject.getDescription() == null ? "" : wsmObject.getDescription();
    workspace.resources = new ArrayList<>();
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
}
