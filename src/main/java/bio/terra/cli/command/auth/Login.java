package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.baseclasses.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth login" command. */
@Command(
    name = "login",
    description = "Authorize the CLI to access Terra APIs and data with user credentials.")
public class Login extends BaseCommand<String> {

  @Override
  protected String execute() {
    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    return "Login successful: " + globalContext.requireCurrentTerraUser().terraUserEmail;
  }
}
