package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFAuthStatus;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.regex.Pattern;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra auth status` command. */
@Tag("unit")
public class AuthStatus extends SingleWorkspaceUnit {
  public static final Pattern VALID_EMAIL_ADDRESS =
      Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

  @Test
  @DisplayName("auth status includes user email and says logged in")
  void authStatusWhenLoggedIn() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");

    // check that it says logged in and includes the user & proxy emails
    assertThat(
        "auth status email matches test user",
        authStatus.userEmail,
        equalToIgnoringCase(testUser.email));
    assertThat(
        "auth status includes proxy group email",
        authStatus.proxyGroupEmail,
        CoreMatchers.not(emptyOrNullString()));
    assertTrue(
        VALID_EMAIL_ADDRESS.matcher(authStatus.proxyGroupEmail).find(),
        "proxy group email is a valid email");
    assertThat(
        "auth status without workspace defined does not include pet SA email",
        authStatus.serviceAccountEmail,
        CoreMatchers.is(emptyOrNullString()));
    assertTrue(authStatus.loggedIn, "auth status indicates user is logged in");
  }

  @Test
  @DisplayName("auth status includes pet SA email when workspace is defined")
  void authStatusWithCurrentWorkspace() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");

    // check that it says logged in and includes the user, proxy group, and pet SA emails
    assertThat(
        "auth status email matches test user",
        authStatus.userEmail,
        equalToIgnoringCase(workspaceCreator.email));
    assertThat(
        "auth status includes proxy group email",
        authStatus.proxyGroupEmail,
        CoreMatchers.not(emptyOrNullString()));
    assertTrue(
        VALID_EMAIL_ADDRESS.matcher(authStatus.proxyGroupEmail).find(),
        "proxy group email is a valid email");
    assertThat(
        "auth status with workspace defined includes pet SA email",
        authStatus.serviceAccountEmail,
        CoreMatchers.not(emptyOrNullString()));
    assertTrue(
        VALID_EMAIL_ADDRESS.matcher(authStatus.serviceAccountEmail).find(),
        "pet SA email is a valid email");
    assertTrue(authStatus.loggedIn, "auth status indicates user is logged in");
  }

  @Test
  @DisplayName("auth status does not include user email and says logged out")
  void authStatusWhenLoggedOut() throws IOException {
    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");

    // check that it says logged out and doesn't include user, proxy group, or pet SA emails
    assertThat(
        "auth status email is empty", authStatus.userEmail, CoreMatchers.is(emptyOrNullString()));
    assertThat(
        "auth status proxy group email is empty",
        authStatus.proxyGroupEmail,
        CoreMatchers.is(emptyOrNullString()));
    assertThat(
        "auth status pet SA email is empty",
        authStatus.serviceAccountEmail,
        CoreMatchers.is(emptyOrNullString()));
    assertFalse(authStatus.loggedIn, "auth status indicates user is logged out");
  }

  @Test
  @DisplayName("auth status changes after logout")
  void authRevokeChanges() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");

    // check that it says logged in
    assertTrue(authStatus.loggedIn, "auth status indicates user is logged in");

    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");

    // `terra auth status --format=json`
    authStatus = TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");

    // check that it says logged out
    assertFalse(authStatus.loggedIn, "auth status indicates user is logged out");
  }
}
