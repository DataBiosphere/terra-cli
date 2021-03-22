package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.TerraUser;
import java.util.Optional;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status extends CommandWithFormatOptions<Status.AuthStatusReturnValue> {

  Optional<TerraUser> currentTerraUserOpt;

  @Override
  protected AuthStatusReturnValue execute() {
    new AuthenticationManager(globalContext, workspaceContext).populateCurrentTerraUser();
    currentTerraUserOpt = globalContext.getCurrentTerraUser();

    // check if current user is defined
    if (!currentTerraUserOpt.isPresent()) {
      return new AuthStatusReturnValue(null, null, false);
    } else {
      TerraUser currentTerraUser = currentTerraUserOpt.get();
      return new AuthStatusReturnValue(
          currentTerraUser.terraUserEmail,
          currentTerraUser.terraProxyGroupEmail,
          !currentTerraUser.requiresReauthentication());
    }
  }

  public static class AuthStatusReturnValue {
    // Terra user email associated with the current user
    public final String userEmail;

    // Terra proxy group email associated with the current user
    public final String proxyGroupEmail;

    // true if the current user does not need to re-authenticate
    public final boolean loggedIn;

    public AuthStatusReturnValue(String userEmail, String proxyGroupEmail, boolean loggedIn) {
      this.userEmail = userEmail;
      this.proxyGroupEmail = proxyGroupEmail;
      this.loggedIn = loggedIn;
    }
  }

  @Override
  protected void printText(AuthStatusReturnValue returnValue) {
    // check if current user is defined
    if (!currentTerraUserOpt.isPresent()) {
      out.println("No current Terra user defined.");
    } else {
      TerraUser currentTerraUser = currentTerraUserOpt.get();
      out.println("Current Terra user: " + currentTerraUser.terraUserEmail);
      out.println("Current Terra user's proxy group: " + currentTerraUser.terraProxyGroupEmail);

      // check if the current user needs to re-authenticate (i.e. is logged out)
      out.println("LOGGED " + (currentTerraUser.requiresReauthentication() ? "OUT" : "IN"));
    }
  }
}
