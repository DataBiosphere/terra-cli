package bio.terra.cli.app.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.VersionCheck;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.SystemVersion;
import java.lang.module.ModuleDescriptor.Version;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionCheckUtils {
  private static final Logger logger = LoggerFactory.getLogger(VersionCheckUtils.class);
  private static final Duration VERSION_CHECK_INTERVAL = Duration.ofMinutes(30);

  /** Query Workspace Manager for the oldest supported */
  public static boolean isObsolete() {
    if (checkIntervalElapsed()) {
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
  public static boolean checkIntervalElapsed() {
    Optional<OffsetDateTime> lastCheckTime =
        Context.getVersionCheck().map(VersionCheck::getLastVersionCheckTime);
    boolean result =
        (lastCheckTime.isEmpty()
            || Duration.between(lastCheckTime.get(), OffsetDateTime.now())
                    .compareTo(VERSION_CHECK_INTERVAL)
                > 0);
    logger.debug(
        "Last version check occurred at {}, which was {} the check interval {} ago.",
        lastCheckTime.map(OffsetDateTime::toString).orElse("never"),
        result ? "greater than" : "less than or equal to",
        VERSION_CHECK_INTERVAL);
    return result;
  }

  /**
   * Look at the semantic version strings and compare them to determine if the current version is
   * older.
   */
  public static boolean isOlder(
      String currentVersionString, @Nullable String oldestSupportedVersionString) {
    if (null == oldestSupportedVersionString) {
      logger.debug(
          "Unable to obtain the oldest supported CLI version from WSM. "
              + "This is OK, as not all deployments expose this value.");
      return false;
    }

    var currentVersion = Version.parse(currentVersionString);
    var oldestSupportedVersion = Version.parse(oldestSupportedVersionString);
    return currentVersion.compareTo(oldestSupportedVersion) < 0;
  }
}
