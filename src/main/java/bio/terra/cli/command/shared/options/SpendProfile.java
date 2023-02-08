package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Server;
import picocli.CommandLine;

/**
 * Command helper class that defines the --profile for `terra spend` and `invite` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class SpendProfile {
  @CommandLine.Option(
      names = "--profile",
      defaultValue = Server.DEFAULT_SPEND_PROFILE,
      description = "The spend profile.")
  public String spendProfile;
}
