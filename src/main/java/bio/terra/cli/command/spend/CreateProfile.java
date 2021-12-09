package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.service.SpendProfileManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend create-profile" command. */
@Command(
    name = "create-profile",
    description = "Create the Workspace Manager default spend profile.")
public class CreateProfile extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(CreateProfile.class);
  /** Create the WSM default spend profile. */
  @Override
  protected void execute() {
    logger.debug("terra spend create-profile");
    SpendProfileManagerService.fromContext().createDefaultSpendProfile();
    OUT.println("Default WSM spend profile created successfully.");
  }
}
