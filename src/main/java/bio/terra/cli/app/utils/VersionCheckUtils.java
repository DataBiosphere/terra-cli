package bio.terra.cli.app.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.SystemVersion;
import java.lang.module.ModuleDescriptor.Version;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionCheckUtils {
  private static final Logger logger = LoggerFactory.getLogger(VersionCheckUtils.class);

  /** Query Workspace Manager for the oldest supported */
  public static boolean isObsolete() {
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

    Version currentVersion = Version.parse(currentVersionString);
    Version oldestSupportedVersion = Version.parse(oldestSupportedVersionString);
    return currentVersion.compareTo(oldestSupportedVersion) < 0;
  }
}
