package bio.terra.cli.command.user;

import bio.terra.cli.businessobject.RegisteredUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFRegisteredUser;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra user status" command. */
@Command(name = "status", description = "Check the registration status of a user.")
public class Status extends BaseCommand {

  @CommandLine.Option(names = "--email", required = true, description = "User email.")
  private String email;

  @CommandLine.Mixin Format formatOption;

  /** Check the registration status of a user. */
  @Override
  protected void execute() {
    RegisteredUser registeredUser = RegisteredUser.getUser(email);
    formatOption.printReturnValue(new UFRegisteredUser(registeredUser), UFRegisteredUser::print);
  }
}
