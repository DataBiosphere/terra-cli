package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth login" command. */
@Command(
    name = "login",
    description = "Authorize the CLI to access Terra APIs and data with user credentials.")
public class Login extends BaseCommand {

  /** Login the user and print out a success message. */
  @Override
  protected void execute() {
    // if the user is already logged in, log them out first
    if (Context.getUser().isPresent()) {
      OUT.println("yuhuyoyo requires log out " + Context.getUser().get().getEmail());
      Context.requireUser().logout();
    }

    User.login();
    OUT.println("Login successful: " + Context.requireUser().getEmail());
  }

  /** Suppress the login by the super class, so that we can logout the user first, if needed. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
