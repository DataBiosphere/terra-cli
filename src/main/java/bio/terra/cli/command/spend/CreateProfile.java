package bio.terra.cli.command.spend;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend create-profile" command. */
@Command(
    name = "create-profile",
    description = "Create the Workspace Manager default spend profile.",
    hidden = true)
public class CreateProfile extends BaseCommand {
  /** Create the WSM default spend profile. */
  @Override
  protected void execute() {
    SpendProfileManagerService.fromContext().createDefaultSpendProfile();
    OUT.println("Default WSM spend profile created successfully.");
  }
}
