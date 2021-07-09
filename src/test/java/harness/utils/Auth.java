package harness.utils;

import bio.terra.cli.serialization.userfacing.UFAuthStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import harness.TestCommand;

/** Utility methods for working with `terra auth` commands. */
public class Auth {
  /**
   * Calls `terra auth status` to get the proxy group email for the current user. Throws an
   * exception if there is no logged in user.
   */
  public static String getProxyGroupEmail() throws JsonProcessingException {
    // `terra auth status --format=json`
    UFAuthStatus authStatus =
        TestCommand.runAndParseCommandExpectSuccess(UFAuthStatus.class, "auth", "status");
    if (!authStatus.loggedIn) {
      throw new RuntimeException("Error getting proxy group email because user is not logged in.");
    }
    return authStatus.proxyGroupEmail;
  }
}
