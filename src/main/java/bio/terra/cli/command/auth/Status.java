package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import bio.terra.cli.context.TerraUser;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status extends CommandSetup {

  @CommandLine.Mixin FormatFlag formatFlag;

  /**
   * Populate the current user in the global context and return an object with a subset of the
   * TerraUser properties.
   */
  @Override
  protected void execute() {
    new AuthenticationManager(globalContext, workspaceContext).populateCurrentTerraUser();
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();

    // check if current user is defined
    AuthStatusReturnValue authStatusReturnValue;
    if (!currentTerraUserOpt.isPresent()) {
      authStatusReturnValue = new AuthStatusReturnValue();
    } else {
      TerraUser currentTerraUser = currentTerraUserOpt.get();
      authStatusReturnValue =
          new AuthStatusReturnValue(
              currentTerraUser.terraUserEmail,
              currentTerraUser.terraProxyGroupEmail,
              !currentTerraUser.requiresReauthentication());
    }

    formatFlag.printReturnValue(authStatusReturnValue, returnValue -> this.printText(returnValue));
  }

  /** POJO class for printing out this command's output. */
  public static class AuthStatusReturnValue {
    // Terra user email associated with the current user
    public final String userEmail;

    // Terra proxy group email associated with the current user
    public final String proxyGroupEmail;

    // true if the current user does not need to re-authenticate
    public final boolean loggedIn;

    // true if there is a current user defined in the global context
    public final boolean currentUserDefined;

    /** Constructor for when there is a current user defined. */
    public AuthStatusReturnValue(String userEmail, String proxyGroupEmail, boolean loggedIn) {
      this.userEmail = userEmail;
      this.proxyGroupEmail = proxyGroupEmail;
      this.loggedIn = loggedIn;
      this.currentUserDefined = true;
    }

    /** Constructor for when there is NOT a current user defined. */
    public AuthStatusReturnValue() {
      this.userEmail = null;
      this.proxyGroupEmail = null;
      this.loggedIn = false;
      this.currentUserDefined = false;
    }
  }

  /**
   * Print this command's output in text format.
   *
   * @param returnValue command return value object
   */
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

  /**
   * This command never requires login.
   *
   * @return false, always
   */
  @Override
  protected boolean doLogin() {
    return false;
  }
}
