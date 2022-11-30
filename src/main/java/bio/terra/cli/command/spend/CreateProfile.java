package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend create-profile" command. */
@Command(name = "create-profile", description = "Create a spend profile.")
public class CreateProfile extends BaseCommand {

  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfile spendProfileOption;

  /** Create a spend profile. */
  @Override
  protected void execute() {
    SpendProfileManagerService.fromContext().createSpendProfile(spendProfileOption.spendProfile);
    OUT.println(spendProfileOption.spendProfile + " spend profile created successfully.");
  }
}
