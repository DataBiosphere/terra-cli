package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.TerraUser;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status extends BaseCommand {

  @CommandLine.Mixin FormatOption formatOption;

  /**
   * Populate the current user in the global context and print out a subset of the TerraUser
   * properties.
   */
  @Override
  protected void execute() {
    new AuthenticationManager(globalContext, workspaceContext).populateCurrentTerraUser();
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();

    // check if current user is defined
    AuthStatusReturnValue authStatusReturnValue;
    if (!currentTerraUserOpt.isPresent()) {
      authStatusReturnValue = AuthStatusReturnValue.createWhenCurrentUserIsUndefined();
    } else {
      TerraUser currentTerraUser = currentTerraUserOpt.get();
      authStatusReturnValue =
          AuthStatusReturnValue.createWhenCurrentUserIsDefined(
              currentTerraUser.terraUserEmail,
              currentTerraUser.terraProxyGroupEmail,
              !currentTerraUser.requiresReauthentication());
    }

    formatOption.printReturnValue(
        authStatusReturnValue, returnValue -> this.printText(returnValue));
  }

  /** POJO class for printing out this command's output. */
  private static class AuthStatusReturnValue {
    // Terra user email associated with the current user
    public final String userEmail;

    // Terra proxy group email associated with the current user
    public final String proxyGroupEmail;

    // true if the current user does not need to re-authenticate
    public final boolean loggedIn;

    // true if there is a current user defined in the global context
    public final boolean currentUserDefined;

    private AuthStatusReturnValue(
        String userEmail, String proxyGroupEmail, boolean loggedIn, boolean currentUserDefined) {
      this.userEmail = userEmail;
      this.proxyGroupEmail = proxyGroupEmail;
      this.loggedIn = loggedIn;
      this.currentUserDefined = currentUserDefined;
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
