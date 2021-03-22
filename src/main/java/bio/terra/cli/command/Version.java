package bio.terra.cli.command;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra version" command. */
@Command(name = "version", description = "Get the installed version.")
public class Version extends CommandWithFormatOptions<String> {

  @Override
  protected String execute() {
    return bio.terra.cli.context.utils.Version.getVersion();
  }

  @Override
  protected boolean doLogin() {
    // command never requires login
    return false;
  }
}
