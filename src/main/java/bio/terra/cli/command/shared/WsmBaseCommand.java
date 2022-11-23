package bio.terra.cli.command.shared;

import bio.terra.cli.app.utils.VersionCheckUtils;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.Main;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.UserIO;
import java.io.PrintStream;
import org.slf4j.LoggerFactory;

public abstract class WsmBaseCommand extends BaseCommand {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WsmBaseCommand.class);
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

    // do the login flow if required
    if (requiresLogin()) {
      User.login();
    } else if (Context.getUser().isPresent()) {
      Context.requireUser().loadExistingCredentials();
    }

    // optionally check if this version of the CLI is out of date
    if (VersionCheckUtils.isObsolete()) {
      ERR.printf(
          "Warning: Version %s of the CLI has expired. Functionality may not work as expected. To install the latest version: curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash ./terra\n"
              + "If you have added the CLI to your $PATH, this step will need to be repeated after the installation is complete.%n",
          bio.terra.cli.utils.Version.getVersion());
      return 0;
    }

    //    // execute the command
    logger.debug("[COMMAND RUN] terra " + String.join(" ", Main.getArgList()));
    execute();

    // set the command exit code
    return 0;
  }
}
