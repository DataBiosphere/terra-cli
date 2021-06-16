package bio.terra.cli.businessobject;

import bio.terra.cli.service.SamService;
import bio.terra.cli.service.SamService.GroupPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a group member (i.e. someone who is a member of a SAM group). This is
 * different from a regular {@link User} because they are never logged in. This is just a reference
 * to another Terra user who has some group membership. This class is not part of the current
 * context or state.
 */
public class GroupMember {
  private static final Logger logger = LoggerFactory.getLogger(GroupMember.class);

  private String email;
  private List<GroupPolicy> policies;

  private GroupMember(String email, List<GroupPolicy> policies) {
    this.email = email;
    this.policies = policies;
  }

  /**
   * Add a user to a SAM group.
   *
   * @param group name of the group
   * @param email email of the user to add
   * @param policy policy to assign the user
   */
  public static GroupMember add(String group, String email, GroupPolicy policy) {
    // call SAM to add a policy + email to the group
    new SamService().addUserToGroup(group, policy, email);
    logger.info("Added user to group: group={}, email={}, policy={}", group, email, policy);

    // return a GroupMember = email + all policies (not just the one that was added here)
    return getUser(group, email);
  }

  /**
   * Remove a user from a SAM group.
   *
   * @param group name of the group
   * @param email email of the user to remove
   * @param policy policy to remove from the user
   */
  public static GroupMember remove(String group, String email, GroupPolicy policy) {
    // call SAM to remove a policy + email from the group
    new SamService().removeUserFromGroup(group, policy, email);
    logger.info("Removed user from group: group={}, email={}, policy={}", group, email, policy);

    // return a GroupMember = email + all policies (not just the one that was removed here)
    return getUser(group, email);
  }

  /** Get the group member object. */
  private static GroupMember getUser(String group, String email) {
    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    return listUsersInMap(group).get(email.toLowerCase());
  }

  /**
   * List the members of the group.
   *
   * @param group name of the group
   * @return a list of group members
   */
  public static List<GroupMember> list(String group) {
    return listUsersInMap(group).values().stream().collect(Collectors.toList());
  }

  /**
   * Get the members of a group in a map, to make it easy to lookup a particular user.
   *
   * @param group name of the group
   * @return a map of email -> group member object
   */
  private static Map<String, GroupMember> listUsersInMap(String group) {
    // call SAM to get the emails + policies for the group
    // convert the SAM objects (policy -> list of emails) to CLI objects (email -> list of policies)
    Map<String, GroupMember> groupMembers = new HashMap<>();
    for (GroupPolicy policy : GroupPolicy.values()) {
      List<String> emailsWithPolicy = new SamService().listUsersInGroup(group, policy);
      for (String email : emailsWithPolicy) {
        // lowercase the email so there is a consistent way of looking up the email address
        // the email address casing in SAM may not match the case of what is provided by the
        // user
        String emailLowercase = email.toLowerCase();
        GroupMember groupMember = groupMembers.get(emailLowercase);
        if (groupMember == null) {
          groupMember = new GroupMember(emailLowercase, new ArrayList<>());
          groupMembers.put(emailLowercase, groupMember);
        }
        groupMember.policies.add(policy);
      }
    }
    return groupMembers;
  }

  // ====================================================
  // Property getters.

  public String getEmail() {
    return email;
  }

  public List<GroupPolicy> getPolicies() {
    return policies;
  }
}
