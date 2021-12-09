package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.service.SpendProfileManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend delete-profile" command. */
@Command(
    name = "delete-profile",
    description = "Delete the Workspace Manager default spend profile.")
public class DeleteProfile extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(DeleteProfile.class);
  @CommandLine.Mixin DeletePrompt deletePromptOption;

  /** Delete the WSM default spend profile. */
  @Override
  protected void execute() {
    logger.debug("terra spend delete-profile");
    deletePromptOption.confirmOrThrow();
    SpendProfileManagerService.fromContext().deleteDefaultSpendProfile();
    OUT.println("Default WSM spend profile deleted successfully.");
  }
}
