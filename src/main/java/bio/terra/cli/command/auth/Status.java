package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFAuthStatus;
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
    UFAuthStatus authStatusReturnValue;
    if (currentUserOpt.isEmpty()) {
      authStatusReturnValue = UFAuthStatus.createWhenCurrentUserIsUndefined();
    } else {
      User currentUser = currentUserOpt.get();
      authStatusReturnValue =
          UFAuthStatus.createWhenCurrentUserIsDefined(
              currentUser.getEmail(),
              currentUser.getProxyGroupEmail(),
              currentUser.getPetSaEmail(),
              !currentUser.requiresReauthentication());
    }

    formatOption.printReturnValue(authStatusReturnValue, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFAuthStatus returnValue) {
    // check if current user is defined
    if (returnValue.userEmail == null) {
      OUT.println("NO USER LOGGED IN");
    } else {
      OUT.println("User email: " + returnValue.userEmail);
      OUT.println("Proxy group email: " + returnValue.proxyGroupEmail);
      OUT.println(
          "Service account email for current workspace: "
              + (returnValue.serviceAccountEmail == null
                  ? "(undefined)"
                  : returnValue.serviceAccountEmail));

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
