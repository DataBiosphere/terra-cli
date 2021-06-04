package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.command.CommandWorkspaceUser;
import bio.terra.workspace.model.IamRole;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands that handle sharing with other users. */
@Tag("unit")
public class WorkspaceUser extends SingleWorkspaceUnit {
  @BeforeEach
  @Override
  protected void setupEachTime() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace list-users --format=json`
    List<CommandWorkspaceUser> listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    for (CommandWorkspaceUser user : listWorkspaceUsers) {
      if (user.email.equalsIgnoreCase(workspaceCreator.email)) {
        continue;
      }
      for (IamRole role : user.roles) {
        // `terra workspace remove-user --email=$email --role=$role
        TestCommand.runCommandExpectSuccess(
            "workspace", "remove-user", "--email=" + user.email, "--role=" + role);
      }
    }

    super.setupEachTime();
  }

  @Test
  @DisplayName("list users includes the workspace creator")
  void listIncludesCreator() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace list-users --format=json`
    List<CommandWorkspaceUser> listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    // check that the workspace creator is in the list as an owner
    Optional<CommandWorkspaceUser> workspaceUser =
        listWorkspaceUsers.stream()
            .filter(user -> user.email.equalsIgnoreCase(workspaceCreator.email))
            .findAny();
    assertTrue(workspaceUser.isPresent(), "workspace creator is in users list");
    assertTrue(workspaceUser.get().roles.contains(IamRole.OWNER), "workspace creator is an owner");
  }

  @Test
  @DisplayName("list users reflects adding a user")
  void listReflectAdd() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUsers testUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER --format=json
    CommandWorkspaceUser addUserReader =
        TestCommand.runCommandExpectSuccess(
            CommandWorkspaceUser.class,
            "workspace",
            "add-user",
            "--email=" + testUser.email,
            "--role=READER",
            "--format=json");

    // check that the user has the READER role
    assertTrue(addUserReader.roles.contains(IamRole.READER), "reader role returned by add-user");

    // `terra workspace list-users --format=json`
    List<CommandWorkspaceUser> listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    // check that the user is in the list as a reader
    Optional<CommandWorkspaceUser> workspaceUser =
        listWorkspaceUsers.stream()
            .filter(user -> user.email.equalsIgnoreCase(testUser.email))
            .findAny();
    assertTrue(workspaceUser.isPresent(), "test user is in users list");
    assertEquals(1, workspaceUser.get().roles.size(), "test user has one role");
    assertTrue(workspaceUser.get().roles.contains(IamRole.READER), "test user has reader role");

    // `terra workspace add-user --email=$email --role=WRITER --format=json
    CommandWorkspaceUser addUserWriter =
        TestCommand.runCommandExpectSuccess(
            CommandWorkspaceUser.class,
            "workspace",
            "add-user",
            "--email=" + testUser.email,
            "--role=WRITER",
            "--format=json");

    // check that the user has both the READER and WRITER roles
    assertTrue(addUserWriter.roles.contains(IamRole.READER), "reader role returned by add-user");
    assertTrue(addUserWriter.roles.contains(IamRole.WRITER), "writer role returned by add-user");

    // `terra workspace list-users --format=json`
    listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    // check that the user is in the list as a reader + writer
    workspaceUser =
        listWorkspaceUsers.stream()
            .filter(user -> user.email.equalsIgnoreCase(testUser.email))
            .findAny();
    assertTrue(workspaceUser.isPresent(), "test user is in users list");
    assertEquals(2, workspaceUser.get().roles.size(), "test user has two roles");
    assertTrue(workspaceUser.get().roles.contains(IamRole.READER), "test user has reader role");
    assertTrue(workspaceUser.get().roles.contains(IamRole.WRITER), "test user has writer role");
  }

  @Test
  @DisplayName("list users reflects removing a user")
  void listReflectRemove() throws IOException {
    // login as the workspace creator and select a test user to share the workspace with
    workspaceCreator.login();
    TestUsers testUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra workspace add-user --email=$email --role=READER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + testUser.email, "--role=READER", "--format=json");

    // `terra workspace add-user --email=$email --role=OWNER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + testUser.email, "--role=OWNER", "--format=json");

    // `terra workspace remove-user --email=$email --role=READER --format=json
    TestCommand.runCommandExpectSuccess(
        "workspace", "remove-user", "--email=" + testUser.email, "--role=READER");

    // `terra workspace list-users --format=json`
    List<CommandWorkspaceUser> listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    // check that the user is in the list as an OWNER only
    Optional<CommandWorkspaceUser> workspaceUser =
        listWorkspaceUsers.stream()
            .filter(user -> user.email.equalsIgnoreCase(testUser.email))
            .findAny();
    assertTrue(workspaceUser.isPresent(), "test user is in users list");
    assertEquals(1, workspaceUser.get().roles.size(), "test user has one role");
    assertTrue(workspaceUser.get().roles.contains(IamRole.OWNER), "test user has owner role");

    // `terra workspace remove-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "remove-user", "--email=" + testUser.email, "--role=OWNER");

    // `terra workspace list-users --format=json`
    listWorkspaceUsers =
        TestCommand.runCommandExpectSuccess(
            new TypeReference<>() {}, "workspace", "list-users", "--format=json");

    // check that the user is not in the list
    workspaceUser =
        listWorkspaceUsers.stream()
            .filter(user -> user.email.equalsIgnoreCase(testUser.email))
            .findAny();
    assertTrue(workspaceUser.isEmpty(), "test user is not in users list");
  }
}
