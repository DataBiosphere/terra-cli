package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend delete-profile" command. */
@Command(
    name = "delete-profile",
    description = "Delete the Workspace Manager default spend profile.",
    hidden = true)
public class DeleteProfile extends BaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;

  /** Delete the WSM default spend profile. */
  @Override
  protected void execute() {
    deletePromptOption.confirmOrThrow();
    SpendProfileManagerService.fromContext().deleteDefaultSpendProfile();
    OUT.println("Default WSM spend profile deleted successfully.");
  }
}
