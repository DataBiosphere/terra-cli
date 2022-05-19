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
import harness.TestUser;
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
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // `terra group create --name=$name`
    String name = SamGroups.randomGroupName();
    UFGroup groupCreated =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "group", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, testUser);

    // check that the name and email match
    assertEquals(name, groupCreated.name, "group name matches after create");
    assertThat(
        "group email contains the name", groupCreated.email, CoreMatchers.containsString(name));
    assertThat(
        "group creator is an admin", groupCreated.currentUserPolicies.contains(GroupPolicy.ADMIN));

    // `terra group list-users --name=$name`
    // check that the group creator is included in the list and is an admin
    expectListedMemberWithPolicies(name, testUser.email, GroupPolicy.ADMIN);

    // `terra group list`
    List<UFGroup> groupList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "group", "list");

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

    // `terra group describe --name=$name`
    UFGroup groupDescribed =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "group", "describe", "--name=" + name);

    // check that the described group matches the created one
    assertEquals(name, groupDescribed.name, "group name matches describe output after create");
    assertEquals(
        groupCreated.email,
        groupDescribed.email,
        "group email matches describe output after create");
    assertTrue(
        groupDescribed.currentUserPolicies.contains(GroupPolicy.ADMIN),
        "group policies for current user matches describe output after create");
    assertEquals(
        1, groupDescribed.numMembers, "group # members matches describe output after create");

    // `terra group delete --name=$name`
    TestCommand.runCommandExpectSuccess("group", "delete", "--name=" + name, "--quiet");

    // `terra group list`
    groupList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "group", "list");

    // check that the group is not included in the list output
    matchedGroup =
        groupList.stream().filter(listedGroup -> listedGroup.name.equals(name)).findAny();
    assertTrue(matchedGroup.isEmpty(), "group does not appear in list after delete");
  }

  @Test
  @DisplayName("describe, delete, list-users, add-user, remove-user all fail with invalid group")
  void invalidGroup() throws IOException {
    TestUser.chooseTestUser().login();
    String badName = "terraCLI_nonexistentGroup";

    // `terra group describe --name=$name`
    TestCommand.Result cmd = TestCommand.runCommand("group", "describe", "--name=" + badName);
    expectGroupNotFound(cmd);

    // `terra group delete --name=$name`
    cmd = TestCommand.runCommand("group", "delete", "--name=" + badName, "--quiet");
    expectGroupNotFound(cmd);

    // `terra group list-users --name=$name`
    cmd = TestCommand.runCommand("group", "list-users", "--name=" + badName);
    expectGroupNotFound(cmd);

    // `terra group add-user --name=$name`
    cmd =
        TestCommand.runCommand(
            "group",
            "add-user",
            "--name=" + badName,
            "--email=" + TestUser.chooseTestUser().email,
            "--policy=MEMBER");
    expectGroupNotFound(cmd);

    // `terra group remove-user --name=$name`
    cmd =
        TestCommand.runCommand(
            "group",
            "remove-user",
            "--name=" + badName,
            "--email=" + TestUser.chooseTestUser().email,
            "--policy=MEMBER");
    expectGroupNotFound(cmd);
  }

  @Test
  @DisplayName("only an admin, not a member, can modify a group")
  void onlyAdminCanModifyGroup() throws IOException {
    TestUser groupCreator = TestUser.chooseTestUser();
    groupCreator.login();

    // `terra group create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("group", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, groupCreator);

    // `terra group add-user --name=$name`
    TestUser groupMember = TestUser.chooseTestUserWhoIsNot(groupCreator);
    TestCommand.runCommandExpectSuccess(
        "group", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=MEMBER");

    // `terra group delete --name=$name` as member
    groupMember.login();
    TestCommand.runCommandExpectExitCode(2, "group", "delete", "--name=" + name, "--quiet");

    // `terra group add-user --email=$email --policy=ADMIN` as member
    TestCommand.runCommandExpectExitCode(
        2, "group", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=ADMIN");

    // `terra group remove-user --email=$email --policy=MEMBER` as member
    TestCommand.runCommandExpectExitCode(
        2,
        "group",
        "remove-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=MEMBER");

    // `terra group delete --name=$name` as admin
    groupCreator.login();
    TestCommand.runCommandExpectSuccess("group", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list-users reflects adding and removing a user")
  void listUsersReflectsAddRemove() throws IOException {
    TestUser groupCreator = TestUser.chooseTestUser();
    groupCreator.login();

    // `terra group create --name=$name`
    String name = SamGroups.randomGroupName();
    TestCommand.runCommandExpectSuccess("group", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, groupCreator);

    // `terra group add-user --name=$name --email=$email --policy=MEMBER`
    TestUser groupMember = TestUser.chooseTestUserWhoIsNot(groupCreator);
    TestCommand.runCommandExpectSuccess(
        "group", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=MEMBER");

    // check that group member is included in the list-users output with one policy
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.MEMBER);

    // `terra group add-user --name=$name --email=$email --policy=ADMIN`
    TestCommand.runCommandExpectSuccess(
        "group", "add-user", "--name=" + name, "--email=" + groupMember.email, "--policy=ADMIN");

    // check that group member is included in the list-users output with two policies
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.MEMBER, GroupPolicy.ADMIN);

    // `terra group describe --name=$name`
    UFGroup groupDescribed =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "group", "describe", "--name=" + name);
    assertEquals(2, groupDescribed.numMembers, "group describe shows two members");

    // test the group list-user as a table format
    TestCommand.Result cmd = TestCommand.runCommand("group", "list-users", "--name=" + name);

    // use regular expression testing the table format and content inside
    assertTrue(cmd.stdErr == null || cmd.stdErr.isEmpty());
    assertEquals(0, cmd.exitCode, "group list-user returned successfully");
    String[] rows = cmd.stdOut.split("\\r?\\n");
    String[] rowHead = rows[0].split("\\s+");
    assertEquals("EMAIL", rowHead[0].trim().replace("\r", ""));
    assertEquals("POLICIES", rowHead[1].trim().replace("\r", ""));

    for (int i = 1; i < rows.length; i = i + 1) {
      String[] rowi = rows[i].split("\\s+", 2);
      assertTrue(
          rowi[0].matches(
              "^[a-zA-Z\\d_-]+(\\.[a-zA-Z\\d_-]+)+@[a-zA-Z\\d_-]+(\\.[a-zA-Z\\d_-]+)+$"));
      assertTrue(
          Arrays.asList("[ADMIN]", "[MEMBER]", "[ADMIN, MEMBER]", "[MEMBER, ADMIN]")
              .contains(rowi[1].trim().replace("\r", "")));
    }

    // `terra group remove-user --name=$name --email=$email --policy=MEMBER`
    TestCommand.runCommandExpectSuccess(
        "group",
        "remove-user",
        "--name=" + name,
        "--email=" + groupMember.email,
        "--policy=MEMBER");

    // check that group member is included in the list-users output with one policy
    expectListedMemberWithPolicies(name, groupMember.email, GroupPolicy.ADMIN);

    // `terra group remove-user --name=$name --email=$email --policy=ADMIN`
    TestCommand.runCommandExpectSuccess(
        "group", "remove-user", "--name=" + name, "--email=" + groupMember.email, "--policy=ADMIN");

    // check that group member is no longer included in the list-users output
    Optional<UFGroupMember> listedGroupMember = listMembersWithEmail(name, groupMember.email);
    assertTrue(listedGroupMember.isEmpty(), "test user is no longer included in members list");

    // check that the group creator is included in the list-users output
    expectListedMemberWithPolicies(name, groupCreator.email, GroupPolicy.ADMIN);

    // `terra group delete --name=$name`
    TestCommand.runCommandExpectSuccess("group", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("cli-testers group includes the appropriate test users")
  void cliTestersGroupMembership() throws IOException {
    // NOTE: this test is checking that test users and spend access are setup as expected for CLI
    // testing. it's not really testing CLI functionality specifically.
    TestUser groupAdmin = TestUser.chooseTestUserWithOwnerAccess();
    groupAdmin.login();

    List<TestUser> expectedGroupMembers =
        TestUser.getTestUsers().stream()
            .filter(
                testUser ->
                    testUser.spendEnabled.equals(TestUser.SpendEnabled.CLI_TEST_USERS_GROUP))
            .collect(Collectors.toList());
    for (TestUser expectedGroupMember : expectedGroupMembers) {
      // check that the test user is included in the list-users output
      expectListedMemberWithPolicies(
          TestUser.CLI_TEST_USERS_GROUP_NAME, expectedGroupMember.email, GroupPolicy.MEMBER);
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
   * Helper method to call `terra group list-users` and filter the results on the specified user
   * email.
   */
  static Optional<UFGroupMember> listMembersWithEmail(String group, String email)
      throws JsonProcessingException {
    // `terra group list-users --format=json`
    List<UFGroupMember> listGroupMembers =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "group", "list-users", "--name=" + group);

    // find the user in the list
    return listGroupMembers.stream().filter(user -> user.email.equalsIgnoreCase(email)).findAny();
  }
}
