package bio.terra.cli.command;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra version" command. */
@Command(name = "version", description = "Get the installed version.")
public class Version extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** Return value is just the version string. */
  @Override
  protected void execute() {
    String version = bio.terra.cli.utils.Version.getVersion();
    formatOption.printReturnValue(version);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
