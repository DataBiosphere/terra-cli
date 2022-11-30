package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend delete-profile" command. */
@Command(name = "delete-profile", description = "Delete a spend profile.")
public class DeleteProfile extends BaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfile spendProfileOption;

  /** Delete a spend profile. */
  @Override
  protected void execute() {
    deletePromptOption.confirmOrThrow();
    SpendProfileManagerService.fromContext().deleteSpendProfile(spendProfileOption.spendProfile);
    OUT.println(spendProfileOption.spendProfile + " spend profile deleted successfully.");
  }
}
