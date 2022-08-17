package unit;

import static bio.terra.cli.businessobject.WorkspaceUser.Role.OWNER;
import static bio.terra.cli.businessobject.WorkspaceUser.Role.READER;
import static bio.terra.cli.businessobject.WorkspaceUser.Role.WRITER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFWorkspaceUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/** Tests for the `terra workspace` commands that handle sharing with other users. */
@Tag("unit")
public class WorkspaceUser extends SingleWorkspaceUnit {
  /**
   * Helper method to check that a workspace user is included in the list with the specified roles.
   * Uses the current workspace.
   */
  private static void expectListedUserWithRoles(
      String userEmail, bio.terra.cli.businessobject.WorkspaceUser.Role... roles)
      throws JsonProcessingException {
    expectListedUserWithRoles(userEmail, null, roles);
  }

  /**
   * Helper method to check that a workspace user is included in the list with the specified roles.
   * Filters on the specified workspace id; Uses the current workspace if null.
   */
  static void expectListedUserWithRoles(
      String userEmail,
      String workspaceUserFacingId,
      bio.terra.cli.businessobject.WorkspaceUser.Role... roles)
      throws JsonProcessingException {
    Optional<UFWorkspaceUser> workspaceUser =
        workspaceListUsersWithEmail(userEmail, workspaceUserFacingId);
    assertTrue(workspaceUser.isPresent(), "test user is in users list");
    assertEquals(
        roles.length, workspaceUser.get().roles.size(), "test user has the right number of roles");
    assertTrue(
        workspaceUser.get().roles.containsAll(Arrays.asList(roles)),
        "test user has the right roles");
  }

  /**
   * Helper method to call `terra workspace list` and filter the results on the specified user
   * email. Uses the current workspace.
   */
  static Optional<UFWorkspaceUser> workspaceListUsersWithEmail(String userEmail)
      throws JsonProcessingException {
    return workspaceListUsersWithEmail(userEmail, null);
  }

  /**
   * Helper method to call `terra workspace list` and filter the results on the specified user email
   * and workspace id (uses the current workspace id if null).
   */
  static Optional<UFWorkspaceUser> workspaceListUsersWithEmail(
      String userEmail, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra workspace list-users --format=json`
    List<UFWorkspaceUser> listWorkspaceUsers =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "workspace", "list-users")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "workspace",
                "list-users",
                "--workspace=" + workspaceUserFacingId);

    // find the user in the list
    return listWorkspaceUsers.stream()
        .filter(user -> user.email.equalsIgnoreCase(userEmail))
        .findAny();
  }

  @BeforeEach
  @Override
  protected void setupEachTime(TestInfo testInfo) throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra workspace list-users --format=json`
    List<UFWorkspaceUser> listWorkspaceUsers =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users");

    for (UFWorkspaceUser user : listWorkspaceUsers) {
      if (user.email.equalsIgnoreCase(workspaceCreator.email)) {
        continue;
      }
      for (bio.terra.cli.businessobject.WorkspaceUser.Role role : user.roles) {
        // `terra workspace remove-user --email=$email --role=$role
        TestCommand.runCommandExpectSuccess(
            "workspace", "remove-user", "--email=" + user.email, "--role=" + role);
      }
    }

    super.setupEachTime(testInfo);
  }

  @Test
  @DisplayName("list users includes the workspace creator")
  void listIncludesCreator() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // check that the workspace creator is in the list as an owner
    expectListedUserWithRoles(workspaceCreator.email, OWNER);
  }

  @Test
  @DisplayName("list users reflects adding a user")
  void listReflectAdd() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUser testUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra workspace add-user --email=$email --role=READER --format=json
    UFWorkspaceUser addUserReader =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspaceUser.class,
            "workspace",
            "add-user",
            "--email=" + testUser.email,
            "--role=READER");

    // check that the user has the READER role
    assertTrue(addUserReader.roles.contains(READER), "reader role returned by add-user");

    // check that the user is in the list as a reader
    expectListedUserWithRoles(testUser.email, READER);

    // `terra workspace add-user --email=$email --role=WRITER --format=json
    UFWorkspaceUser addUserWriter =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspaceUser.class,
            "workspace",
            "add-user",
            "--email=" + testUser.email,
            "--role=WRITER");

    // check that the user has both the READER and WRITER roles
    assertTrue(
        addUserWriter.roles.containsAll(Arrays.asList(READER, WRITER)),
        "reader and writer roles returned by add-user");

    // check that the user is in the list as a reader + writer
    expectListedUserWithRoles(testUser.email, READER, WRITER);
  }

  @Test
  @DisplayName("list users reflects removing a user")
  void listReflectRemove() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUser testUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra workspace add-user --email=$email --role=READER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + testUser.email, "--role=READER", "--format=json");

    // `terra workspace add-user --email=$email --role=OWNER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + testUser.email, "--role=OWNER", "--format=json");

    // `terra workspace remove-user --email=$email --role=READER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "remove-user", "--email=" + testUser.email, "--role=READER");

    // check that the user is in the list as an OWNER only
    expectListedUserWithRoles(testUser.email, OWNER);

    // `terra workspace remove-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "remove-user", "--email=" + testUser.email, "--role=OWNER");

    // check that the user is not in the list
    Optional<UFWorkspaceUser> workspaceUser = workspaceListUsersWithEmail(testUser.email);
    assertTrue(workspaceUser.isEmpty(), "test user is not in users list");
  }
}
