package bio.terra.cli.command.user;

import bio.terra.cli.businessobject.TerraUser;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra user invite" command. */
@Command(name = "invite", description = "Invite a new user.")
public class Invite extends BaseCommand {

  @CommandLine.Option(names = "--email", required = true, description = "User email.")
  private String email;

  /** Invite a new user. */
  @Override
  protected void execute() {
    TerraUser.invite(email);
    OUT.println("Successfully invited user.");
  }
}
