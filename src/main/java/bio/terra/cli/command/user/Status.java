package bio.terra.cli.command.user;

import bio.terra.cli.businessobject.TerraUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFTerraUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra user status" command. */
@Command(name = "status", description = "Check the registration status of a user.")
public class Status extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Status.class);

  @CommandLine.Option(names = "--email", required = true, description = "User email.")
  private String email;

  @CommandLine.Mixin Format formatOption;

  /** Check the registration status of a user. */
  @Override
  protected void execute() {
    logger.debug("terra user status --email={}", email);
    TerraUser terraUser = TerraUser.getUser(email);
    formatOption.printReturnValue(new UFTerraUser(terraUser), UFTerraUser::print);
  }
}
