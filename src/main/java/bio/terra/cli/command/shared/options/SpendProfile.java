package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --profile for `terra spend` and `invite` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class SpendProfile {
  @CommandLine.Option(
      names = "--profile",
      defaultValue = "wm-default-spend-profile",
      description = "The spend profile.")
  public String spendProfile;
}
