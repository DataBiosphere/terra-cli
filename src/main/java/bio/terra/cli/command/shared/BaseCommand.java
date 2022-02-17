package bio.terra.cli.command.shared;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.businessobject.VersionCheck;
import bio.terra.cli.command.Main;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.utils.Logger;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.SystemVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Base class for all commands. This class handles:
 *
 * <p>- reading in the current global and workspace context
 *
 * <p>- setting up logging
 *
 * <p>- executing the command
 *
 * <p>Sub-classes define how to execute the command (i.e. the implementation of {@link #execute}).
 */
@CommandLine.Command
@SuppressFBWarnings(
    value = {"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "MS_CANNOT_BE_FINAL"},
    justification =
        "Output streams are static, so the OUT & ERR properties should also be so. Each command "
            + "execution should only instantiate a single BaseCommand sub-class. So in practice, "
            + "the call() instance method will only be called once, and these static pointers to "
            + "the output streams will only be initialized once. And since picocli handles instantiating "
            + "the command classes, we can't set the output streams in the constructor and make them "
            + "static.")
public abstract class BaseCommand implements Callable<Integer> {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BaseCommand.class);
  private static final Duration VERSION_CHECK_INTERVAL = Duration.ofHours(1);
  // output streams for commands to write to
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

    if (requiresLogin()) {
      // load existing credentials, prompt for login if they don't exist or are expired
      User.login();
    } else if (Context.getUser().isPresent()) {
      Context.requireUser().loadExistingCredentials();
    }

    // execute the command
    logger.debug("[COMMAND RUN] terra " + String.join(" ", Main.getArgList()));
    execute();

    // optionally check if this version of the CLI is out of date
    if (isObsolete()) {
      ERR.printf(
          "The current version of the CLI is out of date. Please upgrade by invoking "
              + "FIXME.%n");
    }

    // set the command exit code
    return 0;
  }

  /**
   * Required override for executing this command and printing any output.
   *
   * <p>Sub-classes should throw exceptions for errors. The Main class handles turning these
   * exceptions into the appropriate exit code.
   */
  protected abstract void execute();

  /**
   * This method returns true if login is required for the command. Default implementation is to
   * always require login.
   *
   * <p>Sub-classes can use the current global and workspace context to decide whether to require
   * login.
   *
   * @return true if login is required for this command
   */
  protected boolean requiresLogin() {
    return true;
  }

  /**
   * Query Workspace Manager for the oldest supported
   *
   * @return
   */
  private boolean isObsolete() {
    if (doVersionCheck()) {
      // update last checked time in the context file
      VersionCheck updatedVersionCheck = new VersionCheck(OffsetDateTime.now());
      Context.setVersionCheck(updatedVersionCheck);

      // The oldest supported version is exposed on the main WSM /version endpoint
      SystemVersion wsmVersion =
          WorkspaceManagerService.unauthenticated(Context.getServer()).getVersion();
      String oldestSupportedVersion = wsmVersion.getOldestSupportedCliVersion();
      String currentCliVersion = bio.terra.cli.utils.Version.getVersion();
      boolean result = isOlder(currentCliVersion, oldestSupportedVersion);
      logger.debug(
          "Current CLI version {} is {} than the oldest supported version {}",
          currentCliVersion,
          result ? "older" : "newer",
          oldestSupportedVersion);
      return result;
    } else {
      return false;
    }
  }

  /**
   * We don't want to hit the version endpoint on the server with every command invocation, so check
   * if a certain amount of time has passed since the last time we checked (or the last time is
   * null/never).
   *
   * @return true if we should do the version check again
   */
  private boolean doVersionCheck() {
    Optional<OffsetDateTime> lastCheckTime =
        Context.getVersionCheck().map(VersionCheck::getLastVersionCheckTime);
    boolean result =
        (lastCheckTime.isEmpty()
            || Duration.between(lastCheckTime.get(), OffsetDateTime.now())
                    .compareTo(VERSION_CHECK_INTERVAL)
                > 0);
    logger.debug(
        "Last version check occurred at {}, which was {} the check interval {} ago.",
        lastCheckTime.orElse(null),
        result ? "greater than" : "less than or equal to",
        VERSION_CHECK_INTERVAL);
    return result;
  }

  /**
   * Look at the semantic version strings and compare them to determine if the current version is
   * older.
   */
  private boolean isOlder(
      String currentVersionString, @Nullable String oldestSupportedVersionString) {
    if (null == oldestSupportedVersionString) {
      logger.debug(
          "Unable to obtain the oldest supported CLI version from WSM. "
              + "This is potentially benign, as not all deployments expose this value.");
      return false;
    }

    var currentVersion = Version.parse(currentVersionString);
    var oldestSupportedVersion = Version.parse(oldestSupportedVersionString);
    return currentVersion.compareTo(oldestSupportedVersion) < 0;
  }
}
