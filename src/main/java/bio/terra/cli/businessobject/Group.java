package bio.terra.cli.businessobject;

import bio.terra.cli.service.SamService;
import bio.terra.cli.service.SamService.GroupPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a SAM group. This class is not part of the current context or state.
 */
public class Group {
  private static final Logger logger = LoggerFactory.getLogger(Group.class);

  private String name;
  private String email;
  private List<GroupPolicy> currentUserPolicies;

  private Group(String name, String email, List<GroupPolicy> currentUserPolicies) {
    this.name = name;
    this.email = email;
    this.currentUserPolicies = currentUserPolicies;
  }

  /** Create a new SAM group. */
  public static Group create(String name) {
    // call SAM to create a new group
    new SamService().createGroup(name);
    logger.info("Created group: name={}", name);

    // return a Group = name + email
    return get(name);
  }

  /** Delete a SAM group. */
  public static Group delete(String name) {
    // call SAM to delete a group
    Group groupToDelete = get(name);
    new SamService().deleteGroup(name);
    logger.info("Deleted group: name={}", name);

    // return a Group = name + email
    return groupToDelete;
  }

  /** Get the group object. */
  public static Group get(String name) {
    return listGroupsInMap().get(name);
  }

  /**
   * List the groups of which the current user is a member.
   *
   * @return a list of groups
   */
  public static List<Group> list() {
    return listGroupsInMap().values().stream().collect(Collectors.toList());
  }

  /**
   * Get the groups in a map, to make it easy to lookup a particular group.
   *
   * @return a map of name -> group object
   */
  private static Map<String, Group> listGroupsInMap() {
    // call SAM to get the list of groups the user is a member of
    List<ManagedGroupMembershipEntry> groupsToPolicies = new SamService().listGroups();

    // convert the SAM objects (group -> policy) to CLI objects (group -> list of policies)
    Map<String, Group> groups = new HashMap<>();
    for (ManagedGroupMembershipEntry groupToPolicy : groupsToPolicies) {
      String name = groupToPolicy.getGroupName();
      String email = groupToPolicy.getGroupEmail();
      Group group = groups.get(name);
      if (group == null) {
        group = new Group(name, email, new ArrayList<>());
        groups.put(name, group);
      }
      GroupPolicy currentUserPolicy = GroupPolicy.fromSamPolicy(groupToPolicy.getRole());
      group.currentUserPolicies.add(currentUserPolicy);
    }
    return groups;
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
}
