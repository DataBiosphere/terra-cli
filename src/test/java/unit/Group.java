package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import bio.terra.cli.service.SamService.GroupPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import harness.utils.SamGroups;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra group` commands. */
@Tag("unit")
public class Group extends ClearContextUnit {
  SamGroups trackedGroups = new SamGroups();

  @AfterAll
  void cleanupOnce() throws IOException {
    // try to delete each group that was created by a method in this class
    trackedGroups.deleteAllTrackedGroups();
  }

  @Test
  @DisplayName("list, describe, list-users reflect creating and deleting a group")
  void listDescribeUsersReflectCreateDelete() throws IOException {
    TestUsers testUser = TestUsers.chooseTestUser();
    testUser.login();

    // `terra groups create --name=$name`
    String name = SamGroups.randomGroupName();
    UFGroup groupCreated =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "groups", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, testUser);

    // check that the name and email match
    assertEquals(name, groupCreated.name, "group name matches after create");
    assertThat(
        "group email contains the name", groupCreated.email, CoreMatchers.containsString(name));
    assertThat(
        "group creator is an admin", groupCreated.currentUserPolicies.contains(GroupPolicy.ADMIN));

    // `terra groups list-users --name=$name`
    // check that the group creator is included in the list and is an admin
    expectListedMemberWithPolicies(name, testUser.email, GroupPolicy.ADMIN);

    // `terra groups list`
    List<UFGroup> groupList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "groups", "list");

    // check that the listed group matches the created one
    Optional<UFGroup> matchedGroup =
        groupList.stream().filter(listedGroup -> listedGroup.name.equals(name)).findAny();
    assertTrue(matchedGroup.isPresent(), "group appears in list after create");
    assertEquals(name, matchedGroup.get().name, "group name matches list output after create");
    assertEquals(
        groupCreated.email,
        matchedGroup.get().email,
        "group email matches list output after create");
    assertTrue(
        matchedGroup.get().currentUserPolicies.contains(GroupPolicy.ADMIN),
        "group policies for current user matches list output after create");

    // `terra groups describe --name=$name`
    UFGroup groupDescribed =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "groups", "describe", "--name=" + name);

    // check that the described group matches the created one
    assertEquals(name, groupDescribed.name, "group name matches describe output after create");
    assertEquals(
        groupCreated.email,
        groupDescribed.email,
        "group email matches describe output after create");
    assertTrue(
        groupDescribed.currentUserPolicies.contains(GroupPolicy.ADMIN),
        "group policies for current user matches describe output after create");

    // `terra groups delete --name=$name`
    UFGroup groupDeleted =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "groups", "delete", "--name=" + name, "--quiet");

    // check that the name and email match, and that the creator was an admin
    assertEquals(name, groupDeleted.name, "group name matches after delete");
    assertThat(
        "group email contained the name", groupDeleted.email, CoreMatchers.containsString(name));
    assertThat(
        "group creator was an admin", groupDeleted.currentUserPolicies.contains(GroupPolicy.ADMIN));

    // `terra groups list`
    groupList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "groups", "list");

    // check that the group is not included in the list output
    matchedGroup =
        groupList.stream().filter(listedGroup -> listedGroup.name.equals(name)).findAny();
    assertTrue(matchedGroup.isEmpty(), "group does not appear in list after delete");
  }

  @Test
  @DisplayName("describe, delete, list-users, add-user, remove-user all fail with invalid group")
  void invalidGroup() throws IOException {
    TestUsers.chooseTestUser().login();
    String badName = "terraCLI_nonexistentGroup";

    // `terra groups describe --name=$name`
    TestCommand.Result cmd = TestCommand.runCommand("groups", "describe", "--name=" + badName);
    expectGroupNotFound(cmd);

    // `terra groups delete --name=$name`
    cmd = TestCommand.runCommand("groups", "delete", "--name=" + badName, "--quiet");
    expectGroupNotFound(cmd);

    // `terra groups list-users --name=$name`
    cmd = TestCommand.runCommand("groups", "list-users", "--name=" + badName);
    expectGroupNotFound(cmd);

    // `terra groups add-user --name=$name`
    cmd =
        TestCommand.runCommand(
            "groups",
            "add-user",
            "--name=" + badName,
            "--email=" + TestUsers.chooseTestUser().email,
            "--policy=MEMBER");
    expectGroupNotFound(cmd);

    // `terra groups remove-user --name=$name`
    cmd =
        TestCommand.runCommand(
            "groups",
            "remove-user",
            "--name=" + badName,
            "--email=" + TestUsers.chooseTestUser().email,
            "--policy=MEMBER");
    expectGroupNotFound(cmd);
  }

  @Test
  @DisplayName("only an admin, not a member, can modify a group")
  void onlyAdminCanModifyGroup() throws IOException {
    TestUsers groupCreator = TestUsers.chooseTestUser();
    groupCreator.login();

    // `terra groups create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("groups", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, groupCreator);

    // `terra groups add-user --name=$name`
    TestUsers groupMember = TestUsers.chooseTestUserWhoIsNot(groupCreator);
    TestCommand.runCommandExpectSuccess(
        "groups", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=MEMBER");

    // `terra groups delete --name=$name` as member
    groupMember.login();
    TestCommand.runCommandExpectExitCode(2, "groups", "delete", "--name=" + name, "--quiet");

    // `terra groups add-user --email=$email --policy=ADMIN` as member
    TestCommand.runCommandExpectExitCode(
        2,
        "groups",
        "add-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=ADMIN");

    // `terra groups remove-user --email=$email --policy=MEMBER` as member
    TestCommand.runCommandExpectExitCode(
        2,
        "groups",
        "remove-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=MEMBER");

    // `terra groups delete --name=$name` as admin
    groupCreator.login();
    TestCommand.runCommandExpectSuccess("groups", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list-users reflects adding and removing a user")
  void listUsersReflectsAddRemove() throws IOException {
    TestUsers groupCreator = TestUsers.chooseTestUser();
    groupCreator.login();

    // `terra groups create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("groups", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, groupCreator);

    // `terra groups add-user --name=$name --email=$email --policy=MEMBER`
    TestUsers groupMember = TestUsers.chooseTestUserWhoIsNot(groupCreator);
    TestCommand.runCommandExpectSuccess(
        "groups", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=MEMBER");

    // check that group member is included in the list-users output with one policy
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.MEMBER);

    // `terra groups add-user --name=$name --email=$email --policy=ADMIN`
    TestCommand.runCommandExpectSuccess(
        "groups", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=ADMIN");

    // check that group member is included in the list-users output with two policies
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.MEMBER, GroupPolicy.ADMIN);

    // `terra groups remove-user --name=$name --email=$email --policy=MEMBER`
    TestCommand.runCommandExpectSuccess(
        "groups",
        "remove-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=MEMBER");

    // check that group member is included in the list-users output with one policy
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.ADMIN);

    // `terra groups remove-user --name=$name --email=$email --policy=ADMIN`
    TestCommand.runCommandExpectSuccess(
        "groups",
        "remove-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=ADMIN");

    // check that group member is no longer included in the list-users output
    Optional<UFGroupMember> listedGroupMember = listMembersWithEmail(name, groupMember.email);
    assertTrue(listedGroupMember.isEmpty(), "test user is no longer included in members list");

    // check that the group creator is included in the list-users output
    expectListedMemberWithPolicies(name, groupCreator.email, GroupPolicy.ADMIN);

    // `terra groups delete --name=$name`
    TestCommand.runCommandExpectSuccess("groups", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("cli-testers group includes the appropriate test users")
  void cliTestersGroupMembership() throws IOException {
    // NOTE: this test is checking that test users and spend access are setup as expected for CLI
    // testing. it's not really testing CLI functionality specifically.
    TestUsers groupAdmin = TestUsers.chooseTestUserWithOwnerAccess();
    groupAdmin.login();

    List<TestUsers> expectedGroupMembers =
        Arrays.asList(TestUsers.values()).stream()
            .filter(
                testUser ->
                    testUser.spendEnabled.equals(TestUsers.SpendEnabled.CLI_TEST_USERS_GROUP))
            .collect(Collectors.toList());
    for (TestUsers expectedGroupMember : expectedGroupMembers) {
      // check that the test user is included in the list-users output
      expectListedMemberWithPolicies(
          TestUsers.CLI_TEST_USERS_GROUP_NAME, expectedGroupMember.email, GroupPolicy.MEMBER);
    }
  }

  /** Helper method to check that a command returned a group not found error. */
  private static void expectGroupNotFound(TestCommand.Result cmd) {
    assertEquals(1, cmd.exitCode, "specifying a nonexistent group threw a UserActionableException");
    assertThat(
        "error message indicates group not found",
        cmd.stdErr,
        CoreMatchers.containsString("No group found with this name"));
  }

  /**
   * Helper method to check that a group member is included in the list with the specified policies.
   */
  private static void expectListedMemberWithPolicies(
      String group, String email, GroupPolicy... policies) throws JsonProcessingException {
    Optional<UFGroupMember> groupMember = listMembersWithEmail(group, email);
    assertTrue(groupMember.isPresent(), "test user is in members list");
    assertEquals(
        policies.length,
        groupMember.get().policies.size(),
        "test user has the right number of policies");
    assertTrue(
        groupMember.get().policies.containsAll(Arrays.asList(policies)),
        "test user has the right policies");
  }

  /**
   * Helper method to call `terra groups list-users` and filter the results on the specified user
   * email.
   */
  static Optional<UFGroupMember> listMembersWithEmail(String group, String email)
      throws JsonProcessingException {
    // `terra groups list-users --format=json`
    List<UFGroupMember> listGroupMembers =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "groups", "list-users", "--name=" + group);

    // find the user in the list
    return listGroupMembers.stream().filter(user -> user.email.equalsIgnoreCase(email)).findAny();
  }
}
