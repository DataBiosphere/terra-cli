package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke extends BaseCommand {

  /** Logout the user and print out a success message. */
  @Override
  protected void execute() {
    new AuthenticationManager(globalContext, workspaceContext).logoutTerraUser();
    OUT.println("Logout successful.");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
