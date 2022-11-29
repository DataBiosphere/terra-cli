package bio.terra.cli.command.shared.options;

import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;

/**
 * Command helper class that defines the options for `terra spend` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class SpendProfileUser {
  @CommandLine.Option(
      names = "--email",
      required = true,
      description = "User (or other group) email.")
  public String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "Spend policy: ${COMPLETION-CANDIDATES}.")
  public SpendProfileManagerService.SpendProfilePolicy policy;

  @CommandLine.Option(
      names = "--profile",
      defaultValue = "wm-default-spend-profile",
      description = "The spend profile.")
  public String spendProfile;
}
