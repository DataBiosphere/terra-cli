package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Server;
import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.service.SpendProfileManagerService.SpendProfilePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.SamGroups;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra spend` commands. */
@Tag("unit")
public class SpendProfileUser extends ClearContextUnit {
  public static final String TEST_SPEND_PROFILE = "test-spend-profile";

  // only an owner on the spend profile can disable emails
  final TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();

  final SamGroups trackedGroups = new SamGroups();

  private static void expectListedUserWithPolicies(
      String email, String profile, SpendProfilePolicy... policies) throws JsonProcessingException {
    Optional<UFSpendProfileUser> spendUser = listUsersWithEmail(email, profile);
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
   * Helper method to check that a spend profile user is included in the list with the specified
   * policies.
   */
  private static void expectListedUserWithPolicies(String email, SpendProfilePolicy... policies)
      throws JsonProcessingException {
    expectListedUserWithPolicies(email, Server.DEFAULT_SPEND_PROFILE, policies);
  }

  static Optional<UFSpendProfileUser> listUsersWithEmail(String email, String profile)
      throws JsonProcessingException {
    // `terra spend list-users --format=json`
    List<UFSpendProfileUser> listSpendUsers =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "spend", "list-users", "--profile=" + profile);

    // find the user in the list
    return listSpendUsers.stream().filter(user -> user.email.equalsIgnoreCase(email)).findAny();
  }

  /**
   * Helper method to call `terra spend list-users` and filter the results on the specified user
   * email.
   */
  static Optional<UFSpendProfileUser> listUsersWithEmail(String email)
      throws JsonProcessingException {
    return listUsersWithEmail(email, Server.DEFAULT_SPEND_PROFILE);
  }

  @AfterAll
  void cleanupOnce() throws IOException, InterruptedException {
    spendProfileOwner.login();

    // try to disable spend for each group that was created by a method in this class
    for (Map.Entry<String, TestUser> groupNameToCreator :
        trackedGroups.getTrackedGroups().entrySet()) {
      UFGroup groupInfo =
          TestCommand.runAndParseCommandExpectSuccess(
              UFGroup.class, "group", "describe", "--name=" + groupNameToCreator.getKey());
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
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
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
  @DisplayName("enable, disable, and list-users with an alternate profile")
  void commandsWithAltProfile() throws IOException {
    // get an email to try and add
    String testEmail = generateSamGroupForEmail();

    // use a test user that is an owner to grant spend access to the test email
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // create test profile if it doesn't exist already
    TestCommand.runCommand("spend", "create-profile", "--profile=" + TEST_SPEND_PROFILE);

    // `terra spend enable --email=$testEmail --policy=USER --profile=TEST_SPEND_PROFILE`
    TestCommand.runCommandExpectSuccess(
        "spend",
        "enable",
        "--email=" + testEmail,
        "--policy=USER",
        "--profile=" + TEST_SPEND_PROFILE);

    // check that the test email is included in the list-users output
    expectListedUserWithPolicies(testEmail, TEST_SPEND_PROFILE, SpendProfilePolicy.USER);

    // `terra spend disable --email=$testEmail --policy=USER --profile=TEST_SPEND_PROFILE`
    TestCommand.runCommandExpectSuccess(
        "spend",
        "disable",
        "--email=" + testEmail,
        "--policy=USER",
        "--profile=" + TEST_SPEND_PROFILE);

    // check that the test email is not included in the list-users output
    Optional<UFSpendProfileUser> listUsersOutput =
        listUsersWithEmail(testEmail, TEST_SPEND_PROFILE);
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
    for (TestUser testUser : TestUser.getTestUsers()) {
      if (testUser.spendEnabled.equals(TestUser.SpendEnabled.OWNER)) {
        continue;
      }
      testUser.login();

      // `terra spend enable --email=$testEmail --policy=USER`
      TestCommand.runCommandExpectExitCode(
          2, "spend", "enable", "--email=" + testEmail, "--policy=USER");
    }

    // use a test user that is an owner to grant spend access to the test email
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();
    TestCommand.runCommandExpectSuccess("spend", "enable", "--email=" + testEmail, "--policy=USER");

    // check that none of the test users can disable this email
    for (TestUser testUser : TestUser.getTestUsers()) {
      if (testUser.spendEnabled.equals(TestUser.SpendEnabled.OWNER)) {
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
  void testUserSpendProfileMembership() throws IOException {
    // NOTE: this test is checking that test users and spend access are setup as expected for CLI
    // testing. it's not really testing CLI functionality specifically.
    // only an owner on the spend profile can list enabled users
    TestUser spendProfileOwner = TestUser.chooseTestUserWithOwnerAccess();
    spendProfileOwner.login();

    // check that the cli-testers group is included in the list-users output
    UFGroup cliTestersGroup =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "group", "describe", "--name=" + TestUser.CLI_TEST_USERS_GROUP_NAME);
    expectListedUserWithPolicies(cliTestersGroup.email, SpendProfilePolicy.USER);

    // check that each test user who is enabled on the spend profile directly, is included in the
    // list-users output
    List<TestUser> expectedSpendUsers =
            TestUser.getTestUsers().stream()
                    .filter(testUser -> testUser.spendEnabled.equals(TestUser.SpendEnabled.DIRECTLY)).toList();
    for (TestUser expectedSpendUser : expectedSpendUsers) {
      expectListedUserWithPolicies(expectedSpendUser.email, SpendProfilePolicy.USER);
    }
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

    // `terra group create --name=$name`
    UFGroup groupCreated =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGroup.class, "group", "create", "--name=" + name);

    // track the group so we can clean it up in case this test method fails
    trackedGroups.trackGroup(name, spendProfileOwner);

    return groupCreated.email;
  }
}
