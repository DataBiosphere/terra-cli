package bio.terra.cli.businessobject;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.RoleBindingList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a workspace user (i.e. someone a workspace is shared with). This is
 * different from a regular {@link User} because they are never logged in. This is just a reference
 * to another Terra user who has some level of workspace access. This class is not part of the
 * current context or state.
 */
public class WorkspaceUser {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceUser.class);

  private String email;
  private List<IamRole> roles;

  private WorkspaceUser(String email, List<IamRole> roles) {
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
    Workspace currentWorkspace = Context.requireWorkspace();

    // call WSM to add a user + role to the current workspace
    new WorkspaceManagerService().grantIamRole(currentWorkspace.getId(), email, role);
    logger.info("Added user to workspace: user={}, role={}", email, role);

    // return a WorkspaceUser = email + all roles (not just the one that was added here)
    return getUser(email);
  }

  /**
   * Remove a user + role from the current workspace. Possible roles are defined by the WSM client
   * library.
   *
   * @param email email of the user to remove
   * @param role role to remove from the user
   * @throws UserActionableException if there is no current workspace
   */
  public static WorkspaceUser remove(String email, IamRole role) {
    Workspace currentWorkspace = Context.requireWorkspace();

    // call WSM to remove a user + role from the current workspace
    new WorkspaceManagerService().removeIamRole(currentWorkspace.getId(), email, role);
    logger.info("Removed user from workspace: user={}, role={}", email, role);

    // return a WorkspaceUser = email + all roles (not just the one that was removed here)
    return getUser(email);
  }

  /** Get the workspace user object in the current workspace. */
  private static WorkspaceUser getUser(String email) {
    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    return listUsersInMap().get(email.toLowerCase());
  }

  /**
   * List the workspace users for the current workspace.
   *
   * @return a list of workspace users
   */
  public static List<WorkspaceUser> list() {
    return listUsersInMap().values().stream().collect(Collectors.toList());
  }

  /**
   * Get the workspace users for the current workspace in a map, to make it easy to lookup a
   * particular user.
   *
   * @return a map of email -> workspace user object
   */
  private static Map<String, WorkspaceUser> listUsersInMap() {
    Workspace currentWorkspace = Context.requireWorkspace();

    // call WSM to get the users + roles for the existing workspace
    RoleBindingList roleBindings = new WorkspaceManagerService().getRoles(currentWorkspace.getId());

    // convert the WSM objects (role -> list of emails) to CLI objects (email -> list of roles)
    Map<String, WorkspaceUser> workspaceUsers = new HashMap<>();
    roleBindings.forEach(
        roleBinding -> {
          IamRole role = roleBinding.getRole();
          for (String email : roleBinding.getMembers()) {
            // lowercase the email so there is a consistent way of looking up the email address
            // the email address casing in SAM may not match the case of what is provided by the
            // user
            String emailLowercase = email.toLowerCase();
            WorkspaceUser workspaceUser = workspaceUsers.get(emailLowercase);
            if (workspaceUser == null) {
              workspaceUser = new WorkspaceUser(emailLowercase, new ArrayList<>());
              workspaceUsers.put(emailLowercase, workspaceUser);
            }
            workspaceUser.roles.add(role);
          }
        });
    return workspaceUsers;
  }

  // ====================================================
  // Property getters.

  public String getEmail() {
    return email;
  }

  public List<IamRole> getRoles() {
    return roles;
  }
}
