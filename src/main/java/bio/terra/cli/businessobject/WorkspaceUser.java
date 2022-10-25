package bio.terra.cli.businessobject;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.RoleBindingList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private List<Role> roles;
  private UUID workspaceId;

  private WorkspaceUser(String email, List<Role> roles) {
    this.email = email.toLowerCase();
    this.roles = roles;
  }

  /**
   * Add a user to a workspace. Possible roles are defined by the WSM client library.
   *
   * @param email email of the user to add
   * @param role role to assign the user
   * @param workspace workspace to add the user to
   * @throws UserActionableException if there is no current workspace
   */
  public static WorkspaceUser add(String email, Role role, Workspace workspace) {
    // call WSM to add a user + role to the workspace
    WorkspaceManagerService.fromContext()
        .grantIamRole(workspace.getUuid(), email, role.getWsmRole());
    logger.info(
        "Added user to workspace: user={}, role={}, workspaceId={}",
        email,
        role,
        workspace.getUuid());

    // return a WorkspaceUser = email + all roles (not just the one that was added here)
    return getUser(email, workspace);
  }

  /**
   * Remove a user + role from a workspace. Possible roles are defined by the WSM client library.
   *
   * @param email email of the user to remove
   * @param role role to remove from the user
   * @param workspace workspace to remove the user from
   * @throws UserActionableException if there is no current workspace
   */
  public static WorkspaceUser remove(String email, Role role, Workspace workspace) {
    // call WSM to remove a user + role from the current workspace
    WorkspaceManagerService.fromContext()
        .removeIamRole(workspace.getUuid(), email, role.getWsmRole());
    logger.info(
        "Removed user from workspace: user={}, role={}, workspaceId={}",
        email,
        role,
        workspace.getUuid());

    // return a WorkspaceUser = email + all roles (not just the one that was removed here)
    return getUser(email, workspace);
  }

  /** Get the workspace user object in a workspace. */
  private static WorkspaceUser getUser(String email, Workspace workspace) {
    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    return listUsersInMap(workspace).get(email.toLowerCase());
  }

  /**
   * List the workspace users for a workspace.
   *
   * @return a list of workspace users
   */
  public static List<WorkspaceUser> list(Workspace workspace) {
    return listUsersInMap(workspace).values().stream().collect(Collectors.toList());
  }

  /**
   * Get the workspace users for a workspace in a map, to make it easy to lookup a particular user.
   *
   * @param workspace workspace to list users in
   * @return a map of email -> workspace user object
   */
  private static Map<String, WorkspaceUser> listUsersInMap(Workspace workspace) {
    // call WSM to get the users + roles for the existing workspace
    RoleBindingList roleBindings =
        WorkspaceManagerService.fromContext().getRoles(workspace.getUuid());

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
            workspaceUser.roles.add(Role.fromWsmRole(role));
          }
        });
    return workspaceUsers;
  }

  public String getEmail() {
    return email;
  }

  // ====================================================
  // Property getters.

  public List<Role> getRoles() {
    return roles;
  }

  public UUID getWorkspaceId() {
    return workspaceId;
  }

  /**
   * Enum for the workspace user roles supported by the CLI. Each enum value maps to a single WSM
   * client library ({@link bio.terra.workspace.model.IamRole}) enum value.
   *
   * <p>The CLI defines its own enum instead of using the WSM one so that we can restrict the roles
   * supported (e.g. no applications). It also gives the CLI control over what the enum names are,
   * which are exposed to users as command options.
   */
  public enum Role {
    READER(IamRole.READER),
    WRITER(IamRole.WRITER),
    OWNER(IamRole.OWNER);

    private IamRole wsmRole;

    Role(IamRole wsmRole) {
      this.wsmRole = wsmRole;
    }

    /** Get the CLI enum value that maps to the given WSM enum value. */
    public static Role fromWsmRole(IamRole wsmRole) {
      return Role.valueOf(wsmRole.getValue());
    }

    /** Get the WSM client enum value that maps to this CLI enum value. */
    public IamRole getWsmRole() {
      return wsmRole;
    }
  }
}
