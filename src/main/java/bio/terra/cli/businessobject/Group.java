package bio.terra.cli.businessobject;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.SamService.GroupPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a SAM group. This class is not part of the current context or state.
 */
public class Group {
  private static final Logger logger = LoggerFactory.getLogger(Group.class);
  private final String name;
  private final String email;
  private final List<GroupPolicy> currentUserPolicies;

  private Group(String name, String email, List<GroupPolicy> currentUserPolicies) {
    this.name = name;
    this.email = email;
    this.currentUserPolicies = currentUserPolicies;
  }

  /** Create a new SAM group. */
  public static Group create(String name) {
    SamService.fromContext().createGroup(name);
    logger.info("Created group: name={}", name);
    return get(name);
  }

  /**
   * Get the group object.
   *
   * @throws UserActionableException if the group is not found
   */
  public static Group get(String name) {
    Group foundGroup = listGroupsInMap().get(name);
    if (foundGroup == null) {
      throw new UserActionableException("No group found with this name: " + name);
    }
    return foundGroup;
  }

  /**
   * List the groups of which the current user is a member.
   *
   * @return a list of groups
   */
  public static List<Group> list() {
    return new ArrayList<>(listGroupsInMap().values());
  }

  /**
   * Get the groups in a map, to make it easy to lookup a particular group.
   *
   * @return a map of name -> group object
   */
  private static Map<String, Group> listGroupsInMap() {
    // call SAM to get the list of groups the user is a member of
    List<ManagedGroupMembershipEntry> groupsToPolicies = SamService.fromContext().listGroups();

    // convert the SAM objects (group -> policy) to CLI objects (group -> list of policies)
    Map<String, Group> nameToGroup = new HashMap<>();
    for (ManagedGroupMembershipEntry groupToPolicy : groupsToPolicies) {
      String name = groupToPolicy.getGroupName();
      String email = groupToPolicy.getGroupEmail();
      Group group = nameToGroup.get(name);
      if (group == null) {
        group = new Group(name, email, new ArrayList<>());
        nameToGroup.put(name, group);
      }
      GroupPolicy currentUserPolicy = GroupPolicy.fromSamPolicy(groupToPolicy.getRole());
      group.addCurrentUserPolicy(currentUserPolicy);
    }
    return nameToGroup;
  }

  /** Delete a SAM group. */
  public void delete() {
    validateGroupAdmin();
    SamService.fromContext().deleteGroup(name);
    logger.info("Deleted group: name={}", name);
  }

  /**
   * Add a member to a SAM group.
   *
   * @param email email of the user to add
   * @param policy policy to assign the user
   */
  public Member addPolicyToMember(String email, GroupPolicy policy) {
    validateGroupAdmin();
    // call SAM to add a policy + email to the group
    SamService.fromContext().addUserToGroup(name, policy, email);
    logger.info("Added user to group: group={}, email={}, policy={}", name, email, policy);

    // return a GroupMember = email + all policies (not just the one that was added here)
    return getMember(email);
  }

  /**
   * Remove a member from a SAM group policy.
   *
   * @param email email of the user to remove
   * @param policy policy to remove from the user
   */
  public void removePolicyFromMember(String email, GroupPolicy policy) {
    validateGroupAdmin();
    // check that the email is a group member
    getMember(email);

    // call SAM to remove a policy + email from the group
    SamService.fromContext().removeUserFromGroup(name, policy, email);
    logger.info("Removed user from group: group={}, email={}, policy={}", name, email, policy);
  }

  /**
   * Get the group member object.
   *
   * @throws UserActionableException if the member is not found
   */
  private Member getMember(String email) {
    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    Member foundMember = listMembersByEmail().get(email.toLowerCase());
    if (foundMember == null) {
      throw new UserActionableException("No group member found with this email: " + email);
    }
    return foundMember;
  }

  /** List the members of the group. */
  public List<Member> getMembers() {
    validateGroupAdmin();
    return new ArrayList<>(listMembersByEmail().values());
  }

  /**
   * Get the members of a group in a map, to make it easy to lookup a particular user.
   *
   * @return a map of email -> group member object
   */
  private Map<String, Member> listMembersByEmail() {
    // call SAM to get the emails + policies for the group
    // convert the SAM objects (policy -> list of emails) to CLI objects (email -> list of policies)
    Map<String, Member> groupMembers = new HashMap<>();
    for (GroupPolicy policy : GroupPolicy.values()) {
      List<String> emailsWithPolicy = SamService.fromContext().listUsersInGroup(name, policy);
      for (String email : emailsWithPolicy) {
        // lowercase the email so there is a consistent way of looking up the email address
        // the email address casing in SAM may not match the case of what is provided by the
        // user
        String emailLowercase = email.toLowerCase();
        Member groupMember = groupMembers.get(emailLowercase);
        if (groupMember == null) {
          groupMember = new Member(emailLowercase, new ArrayList<>());
          groupMembers.put(emailLowercase, groupMember);
        }
        groupMember.addPolicy(policy);
      }
    }
    return groupMembers;
  }

  /**
   * Throw a user-actionable exception if the current user is not an admin of this group, which is
   * required for some operations.
   */
  private void validateGroupAdmin() {
    if (!currentUserPolicies.contains(GroupPolicy.ADMIN)) {
      throw new UserActionableException(
          String.format(
              "Cannot view or modify membership of group %s, user is not an administrator of this group.",
              name));
    }
  }
  // ====================================================
  // Property getters.

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public List<GroupPolicy> getCurrentUserPolicies() {
    return currentUserPolicies;
  }

  public void addCurrentUserPolicy(GroupPolicy policy) {
    currentUserPolicies.add(policy);
  }

  /**
   * Internal representation of a group member (i.e. someone who is a member of a SAM group). This
   * is different from a regular {@link User} because they are never logged in. This is just a
   * reference to another Terra user who has some group membership. This class is not part of the
   * current context or state.
   */
  public static class Member {
    private final String email;
    private final List<GroupPolicy> policies;

    private Member(String email, List<GroupPolicy> policies) {
      this.email = email;
      this.policies = policies;
    }

    public String getEmail() {
      return email;
    }

    public List<GroupPolicy> getPolicies() {
      return policies;
    }

    public void addPolicy(GroupPolicy policy) {
      policies.add(policy);
    }
  }
}
