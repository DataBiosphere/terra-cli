package bio.terra.cli.command.shared;

import bio.terra.cli.app.utils.VersionCheckUtils;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.UserIO;
import java.io.PrintStream;

/**
 * This class prints a warning if CLI version is too old for WSM version.
 *
 * <p>Commands which call WSM should extend this instead of BaseCommand. (This way, if WSM happens
 * to be down, commands that don't call WSM will still succeed.)
 */
public abstract class WsmBaseCommand extends BaseCommand {
  protected static PrintStream OUT;
  protected static PrintStream ERR;

  @Override
  public Integer call() {
    // pull the output streams from the singleton object setup by the top-level Main class
    // in the future, these streams could also be controlled by a global context property
    OUT = UserIO.getOut();
    ERR = UserIO.getErr();

    // read in the global context and setup logging
    Context.initializeFromDisk();
    Logger.setupLogging(
        Context.getConfig().getConsoleLoggingLevel(), Context.getConfig().getFileLoggingLevel());

    // Check if this version of the CLI is out of date
    if (VersionCheckUtils.isObsolete()) {
      ERR.printf(
          "Warning: Version %s of the CLI has expired. Functionality may not work as expected. To install the latest version: curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash ./terra\n"
              + "If you have added the CLI to your $PATH, this step will need to be repeated after the installation is complete.%n",
          bio.terra.cli.utils.Version.getVersion());
      return 0;
    }

    return super.call();
  }
}
