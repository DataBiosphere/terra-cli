package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.RoleBindingList;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POJO class that represents a workspace user (i.e. someone a workspace is shared with). This is
 * different from a regular {@link TerraUser} because they are never logged in. This is just a
 * reference to another Terra user who has some level of workspace access. This class is not
 * serialized to disk as part of the global context. It is used as the user-facing JSON output for
 * commands that return a workspace user.
 */
public class WorkspaceUser {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceUser.class);

  public final String email;
  private List<IamRole> roles;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  private WorkspaceUser(
      @JsonProperty("email") String email, @JsonProperty("roles") List<IamRole> roles) {
    this.email = email;
    this.roles = roles;
  }

  /**
   * Add a user to the current workspace. Possible roles are defined by the WSM client library.
   *
   * @param email email of the user to add
   * @param role role to assign the user
   * @throws UserActionableException if there is no current workspace
   */
  public static WorkspaceUser add(String email, IamRole role) {
    Workspace currentWorkspace = GlobalContext.get().requireCurrentWorkspace();

    // call WSM to add a user + role to the current workspace
    new WorkspaceManagerService().grantIamRole(currentWorkspace.id, email, role);
    logger.info("Added user to workspace: user={}, role={}", email, role);

    // return a WorkspaceUser that includes all roles this email has
    return list().get(email);
  }

  /**
   * Remove a user + role from the current workspace. Possible roles are defined by the WSM client
   * library.
   *
   * @param email email of the user to add
   * @param role role to assign the user
   * @throws UserActionableException if there is no current workspace
   */
  public static WorkspaceUser remove(String email, IamRole role) {
    Workspace currentWorkspace = GlobalContext.get().requireCurrentWorkspace();

    // call WSM to remove a user + role from the current workspace
    new WorkspaceManagerService().removeIamRole(currentWorkspace.id, email, role);
    logger.info("Removed user from workspace: user={}, role={}", email, role);

    // return a WorkspaceUser that includes all roles this email has
    return list().get(email);
  }

  /**
   * List the workspace users for the current workspace.
   *
   * @return a list of workspace users
   */
  public static Map<String, WorkspaceUser> list() {
    Workspace currentWorkspace = GlobalContext.get().requireCurrentWorkspace();

    // call WSM to get the users + roles for the existing workspace
    RoleBindingList roleBindings = new WorkspaceManagerService().getRoles(currentWorkspace.id);

    // convert the WSM objects (role -> list of emails) to CLI objects (email -> list of roles)
    Map<String, WorkspaceUser> workspaceUsers = new HashMap<>();
    roleBindings.forEach(
        roleBinding -> {
          IamRole role = roleBinding.getRole();
          for (String email : roleBinding.getMembers()) {
            WorkspaceUser workspaceUser = workspaceUsers.get(email);
            if (workspaceUser == null) {
              workspaceUser = new WorkspaceUser(email, new ArrayList<>());
              workspaceUsers.put(email, workspaceUser);
            }
            workspaceUser.roles.add(role);
          }
        });
    return workspaceUsers;
  }

  /** Print out a workspace user object in text format. */
  public void printText() {
    List<String> rolesStr = roles.stream().map(IamRole::toString).collect(Collectors.toList());
    Printer.getOut().println(email + ": " + String.join(",", rolesStr));
  }
}
