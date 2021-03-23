package bio.terra.cli.command;

import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra version" command. */
@Command(name = "version", description = "Get the installed version.")
public class Version extends CommandSetup {

  @CommandLine.Mixin FormatFlag formatFlag;

  /** Return value is just the version string. */
  @Override
  protected void execute() {
    String versionReturnValue = bio.terra.cli.context.utils.Version.getVersion();
    formatFlag.printReturnValue(versionReturnValue);
  }

  /**
   * This command never requires login.
   *
   * @return false, always
   */
  @Override
  protected boolean doLogin() {
    return false;
  }
}
