package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.service.SpendProfileManagerService.SpendProfilePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import harness.utils.SamGroups;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra spend` commands. */
@Tag("unit")
public class SpendProfileUser extends ClearContextUnit {
  // only an owner on the spend profile can disable emails
  TestUsers spendProfileOwner = TestUsers.chooseTestUserWithOwnerAccess();

  SamGroups trackedGroups = new SamGroups();

  @AfterAll
  void cleanupOnce() throws IOException, InterruptedException {
    spendProfileOwner.login();

    // try to disable spend for each group that was created by a method in this class
    for (Map.Entry<String, TestUsers> groupNameToCreator :
        trackedGroups.getTrackedGroups().entrySet()) {
      UFGroup groupInfo =
          TestCommand.runAndParseCommandExpectSuccess(
              UFGroup.class, "groups", "describe", "--name=" + groupNameToCreator.getKey());
      TestCommand.runCommand("spend", "disable", "--email=" + groupInfo.email, "--policy=USER");
      TestCommand.runCommand("spend", "disable", "--email=" + groupInfo.email, "--policy=OWNER");
    }

    // then try to delete the groups
    trackedGroups.deleteAllTrackedGroups();
  }

  @Test
  @DisplayName("list-users reflects enabling and disabling users")
  void listUsersReflectsEnableDisable() throws IOException {
    // get an email to try and add
    String testEmail = generateSamGroupForEmail();

    // use a test user that is an owner to grant spend access to the test email
    TestUsers spendProfileOwner = TestUsers.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // `terra spend enable --email=$testEmail --policy=USER`
    TestCommand.runCommandExpectSuccess("spend", "enable", "--email=" + testEmail, "--policy=USER");

    // check that the test email is included in the list-users output
    expectListedUserWithPolicies(testEmail, SpendProfilePolicy.USER);

    // `terra spend disable --email=$testEmail --policy=USER`
    TestCommand.runCommandExpectSuccess(
        "spend", "disable", "--email=" + testEmail, "--policy=USER");

    // check that the test email is not included in the list-users output
    Optional<UFSpendProfileUser> listUsersOutput = listUsersWithEmail(testEmail);
    assertTrue(
        listUsersOutput.isEmpty(),
        "user is not included in the test-users output after having their spend access disabled");
  }

  @Test
  @DisplayName("only users who are an admin on the spend profile can enable and disable others")
  void onlyAdminCanEnableDisable() throws IOException {
    // get an email to try and add
    String testEmail = generateSamGroupForEmail();

    // check that none of the test users can enable this email
    for (TestUsers testUser : TestUsers.values()) {
      if (testUser.spendEnabled.equals(TestUsers.SpendEnabled.OWNER)) {
        continue;
      }
      testUser.login();

      // `terra spend enable --email=$testEmail --policy=USER`
      TestCommand.runCommandExpectExitCode(
          2, "spend", "enable", "--email=" + testEmail, "--policy=USER");
    }

    // use a test user that is an owner to grant spend access to the test email
    TestUsers spendProfileOwner = TestUsers.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();
    TestCommand.runCommandExpectSuccess("spend", "enable", "--email=" + testEmail, "--policy=USER");

    // check that none of the test users can disable this email
    for (TestUsers testUser : TestUsers.values()) {
      if (testUser.spendEnabled.equals(TestUsers.SpendEnabled.OWNER)) {
        continue;
      }
      testUser.login();

      // `terra spend disable --email=$testEmail --policy=USER`
      TestCommand.runCommandExpectExitCode(
          2, "spend", "disable", "--email=" + testEmail, "--policy=USER");
    }

    // use a test user that is an owner to remove spend access for the test email
    spendProfileOwner.login();
    TestCommand.runCommandExpectSuccess(
        "spend", "disable", "--email=" + testEmail, "--policy=USER");
  }

  @Test
  @DisplayName("spend profile includes cli-testers group and the appropriate test users")
  void testUsersSpendProfileMembership() throws IOException {
    // only an owner on the spend profile can list enabled users
    TestUsers spendProfileOwner = TestUsers.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // check that the cli-testers group is included in the list-users output
    UFGroup cliTestersGroup =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "groups", "describe", "--name=" + TestUsers.CLI_TEST_USERS_GROUP_NAME);
    expectListedUserWithPolicies(cliTestersGroup.email, SpendProfilePolicy.USER);

    // check that each test user who is enabled on the spend profile directly, is included in the
    // list-users output
    List<TestUsers> expectedSpendUsers =
        Arrays.asList(TestUsers.values()).stream()
            .filter(testUser -> testUser.spendEnabled.equals(TestUsers.SpendEnabled.DIRECTLY))
            .collect(Collectors.toList());
    for (TestUsers expectedSpendUser : expectedSpendUsers) {
      expectListedUserWithPolicies(expectedSpendUser.email, SpendProfilePolicy.USER);
    }
  }

  /**
   * Helper method to check that a spend profile user is included in the list with the specified
   * policies.
   */
  private static void expectListedUserWithPolicies(String email, SpendProfilePolicy... policies)
      throws JsonProcessingException {
    Optional<UFSpendProfileUser> spendUser = listUsersWithEmail(email);
    assertTrue(spendUser.isPresent(), "test user is in spend users list");
    assertEquals(
        policies.length,
        spendUser.get().policies.size(),
        "test user has the right number of policies");
    assertTrue(
        spendUser.get().policies.containsAll(Arrays.asList(policies)),
        "test user has the right policies");
  }

  /**
   * Helper method to call `terra spend list-users` and filter the results on the specified user
   * email.
   */
  static Optional<UFSpendProfileUser> listUsersWithEmail(String email)
      throws JsonProcessingException {
    // `terra spend list-users --format=json`
    List<UFSpendProfileUser> listSpendUsers =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "spend", "list-users");

    // find the user in the list
    return listSpendUsers.stream().filter(user -> user.email.equalsIgnoreCase(email)).findAny();
  }

  /**
   * Create a SAM group, so we can use its generated email. This is a workaround to our current use
   * of hard-coded test users. We don't want to use the CLI test users here because then this test
   * could not be run simultaneously with other tests.
   *
   * @return the generated group's email
   */
  private String generateSamGroupForEmail() throws IOException {
    String name = SamGroups.randomGroupName();
    spendProfileOwner.login();

    // `terra groups create --name=$name`
    UFGroup groupCreated =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "groups", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, spendProfileOwner);

    return groupCreated.email;
  }
}
