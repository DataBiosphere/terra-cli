package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke extends BaseCommand {

  /** Logout the user and print out a success message. */
  @Override
  protected void execute() {
    if (Context.getUser().isPresent()) {
      Context.requireUser().logout();
      OUT.println("Logout successful.");
    } else {
      OUT.println("No user logged in.");
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
