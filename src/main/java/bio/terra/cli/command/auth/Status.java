package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /**
   * Populate the current user in the global context and print out a subset of the TerraUser
   * properties.
   */
  @Override
  protected void execute() {
    // check if current user is defined
    Optional<User> currentUserOpt = Context.getUser();
    AuthStatusReturnValue authStatusReturnValue;
    if (currentUserOpt.isEmpty()) {
      authStatusReturnValue = AuthStatusReturnValue.createWhenCurrentUserIsUndefined();
    } else {
      User currentUser = currentUserOpt.get();
      authStatusReturnValue =
          AuthStatusReturnValue.createWhenCurrentUserIsDefined(
              currentUser.getEmail(),
              currentUser.getProxyGroupEmail(),
              !currentUser.requiresReauthentication());
    }

    formatOption.printReturnValue(
        authStatusReturnValue, returnValue -> this.printText(returnValue));
  }

  /** POJO class for printing out this command's output. */
  @VisibleForTesting
  public static class AuthStatusReturnValue {
    // Terra user email associated with the current user
    public String userEmail;

    // Terra proxy group email associated with the current user
    public String proxyGroupEmail;

    // true if the current user does not need to re-authenticate
    public boolean loggedIn;

    // public constructor for Jackson serialization
    public AuthStatusReturnValue() {}

    private AuthStatusReturnValue(
        String userEmail, String proxyGroupEmail, boolean loggedIn, boolean currentUserDefined) {
      this.userEmail = userEmail;
      this.proxyGroupEmail = proxyGroupEmail;
      this.loggedIn = loggedIn;
    }

    /** Constructor for when there is a current user defined. */
    public static AuthStatusReturnValue createWhenCurrentUserIsDefined(
        String userEmail, String proxyGroupEmail, boolean loggedIn) {
      return new AuthStatusReturnValue(userEmail, proxyGroupEmail, loggedIn, true);
    }

    /** Constructor for when there is NOT a current user defined. */
    public static AuthStatusReturnValue createWhenCurrentUserIsUndefined() {
      return new AuthStatusReturnValue(null, null, false, false);
    }
  }

  /** Print this command's output in text format. */
  private void printText(AuthStatusReturnValue returnValue) {
    // check if current user is defined
    if (returnValue.userEmail == null) {
      OUT.println("No current Terra user defined.");
    } else {
      OUT.println("Current Terra user: " + returnValue.userEmail);
      OUT.println("Current Terra user's proxy group: " + returnValue.proxyGroupEmail);

      // check if the current user needs to re-authenticate (i.e. is logged out)
      OUT.println("LOGGED " + (returnValue.loggedIn ? "IN" : "OUT"));
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
