package bio.terra.cli.command.auth;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Revoke.class);

  /** Logout the user and print out a success message. */
  @Override
  protected void execute() {
    logger.debug("terra auth revoke");
    Context.getUser().ifPresent(User::logout);
    OUT.println("Logout successful.");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
