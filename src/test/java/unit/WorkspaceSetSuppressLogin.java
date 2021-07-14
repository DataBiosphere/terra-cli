package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFAuthStatus;
import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `--suppress-login` option to skip login when setting the current workspace. */
@Tag("unit")
public class WorkspaceSetSuppressLogin extends SingleWorkspaceUnit {
  TestUsers workspaceSharee;
  UUID sharedWorkspaceId;

  @BeforeAll
  protected void setupOnce() throws IOException {
    super.setupOnce();

    // `terra workspace create --format=json`
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    sharedWorkspaceId = createWorkspace.id;

    workspaceSharee = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);

    // `terra workspace add-user --email=$sharee --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + workspaceSharee.email, "--role=READER");
  }

  @AfterAll
  protected void cleanupOnce() throws IOException {
    super.cleanupOnce();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + sharedWorkspaceId);

    // `terra workspace delete --quiet`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("workspace id can be set before logging in, and metadata loads after logging in")
  void workspaceLoadsOnlyAfterLogin() throws IOException {
    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");

    // `terra workspace set --id=$id --suppress-login`
    UFWorkspace workspaceSet =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId(), "--suppress-login");
    assertEquals(
        getWorkspaceId(), workspaceSet.id, "workspace set before login includes workspace id");
    assertNull(
        workspaceSet.googleProjectId,
        "workspace set before login does not include google project id");

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(
        getWorkspaceId(), status.workspace.id, "status before login includes workspace id");
    assertNull(
        status.workspace.googleProjectId, "status before login does not include google project id");

    // `terra auth status`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertNull(authStatus.userEmail, "auth status before login does not include user email");
    assertNull(
        authStatus.serviceAccountEmail, "auth status before login does not include pet SA email");

    workspaceCreator.login();

    // `terra status`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(getWorkspaceId(), status.workspace.id, "status after login includes workspace id");
    assertNotNull(
        status.workspace.googleProjectId, "status after login includes google project id");

    // `terra auth status`
    authStatus = TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertNotNull(authStatus.userEmail, "auth status after login includes user email");
    assertNotNull(authStatus.serviceAccountEmail, "auth status after login includes pet SA email");
  }

  @Test
  @DisplayName(
      "workspace metadata fails to load after logging in as a user without read access, then succeeds with a different workspace that they do have access to")
  void workspaceLoadFailsWithNoAccess() throws IOException {
    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");

    // `terra workspace set --id=$id --suppress-login`
    TestCommand.runCommandExpectSuccess(
        "workspace", "set", "--id=" + getWorkspaceId(), "--suppress-login");

    // the login command should throw an exception that the user doesn't have access to the
    // workspace, but it should also succeed in that the user is logged in afterwards (the pet SA
    // just could not be loaded). here we check an exception instead of a command exit code, because
    // test users bypass the login flow to avoid browser interaction.
    workspaceSharee.login();

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(
        getWorkspaceId(),
        status.workspace.id,
        "status after login user without access includes workspace id");
    assertNull(
        status.workspace.googleProjectId,
        "status after login user without access does not include google project id");

    // `terra auth status`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertNotNull(
        authStatus.userEmail, "auth status after login user without access includes user email");
    assertNull(
        authStatus.serviceAccountEmail,
        "auth status after login user without access does not include pet SA email");

    // `terra resources list`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "resources", "list");
    assertThat(
        "error message includes unauthorized to read workspace resource",
        stdErr,
        CoreMatchers.containsStringIgnoringCase(
            "User "
                + authStatus.userEmail
                + " is not authorized to read resource "
                + getWorkspaceId()
                + " of type workspace"));

    // `terra workspace set --id=$sharedId`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + sharedWorkspaceId);

    // `terra status`
    status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(
        sharedWorkspaceId,
        status.workspace.id,
        "status after login user with access includes shared workspace id");
    assertNotNull(
        status.workspace.googleProjectId,
        "status after login user with access includes google project id");

    // `terra auth status`
    authStatus = TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertNotNull(
        authStatus.userEmail, "auth status after login user with access includes user email");
    assertNotNull(
        authStatus.serviceAccountEmail,
        "auth status after login user with access includes pet SA email");

    TestCommand.runCommandExpectSuccess("resources", "list");
  }

  @Test
  @DisplayName("suppress login flag does not have any effect if user is already logged in")
  void workspaceLoadsImmediatelyWhenAlreadyLoggedIn() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --suppress-login`
    UFWorkspace workspaceSet =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId(), "--suppress-login");
    assertEquals(
        getWorkspaceId(), workspaceSet.id, "workspace set after login includes workspace id");
    assertNotNull(
        workspaceSet.googleProjectId, "workspace set after login includes google project id");

    // `terra status`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");
    assertEquals(getWorkspaceId(), status.workspace.id, "status after login includes workspace id");
    assertNotNull(
        status.workspace.googleProjectId, "status after login includes google project id");

    // `terra auth status`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    assertNotNull(authStatus.userEmail, "auth status after login includes user email");
    assertNotNull(authStatus.serviceAccountEmail, "auth status after login includes pet SA email");

    // `terra resources list`
    TestCommand.runCommandExpectSuccess("resources", "list");
  }

  @Test
  @DisplayName("workspace set without flag still prompts for login")
  void withoutFlagWorkspaceSetRequiresLogin() {
    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");

    // `terra config set browser MANUAL`
    TestCommand.runCommandExpectSuccess("config", "set", "browser", "MANUAL");

    // `terra workspace set --id=$id`
    ByteArrayInputStream stdIn =
        new ByteArrayInputStream("invalid oauth code".getBytes(StandardCharsets.UTF_8));
    TestCommand.Result cmd =
        TestCommand.runCommand(stdIn, "workspace", "set", "--id=" + getWorkspaceId());
    assertThat(
        "stdout includes login prompt",
        cmd.stdOut,
        CoreMatchers.containsString(
            "Please open the following address in a browser on any machine"));
  }
}
