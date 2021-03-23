package bio.terra.cli.command.auth;

import bio.terra.cli.command.helperclasses.CommandSetup;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth login" command. */
@Command(
    name = "login",
    description = "Authorize the CLI to access Terra APIs and data with user credentials.")
public class Login extends CommandSetup {

  /** Login the user and print out a success message. */
  @Override
  protected void execute() {
    // the base command class will always login the user unless the {@link BaseCommand#doLogin}
    // method is overridden
    out.println("Login successful: " + globalContext.requireCurrentTerraUser().terraUserEmail);
  }
}
