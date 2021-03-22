package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.baseclasses.BaseCommand;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke extends BaseCommand<String> {

  @Override
  protected String execute() {
    new AuthenticationManager(globalContext, workspaceContext).logoutTerraUser();
    return "Logout successful.";
  }
}
