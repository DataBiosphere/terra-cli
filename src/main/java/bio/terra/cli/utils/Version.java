package bio.terra.cli.utils;

import org.slf4j.LoggerFactory;

/** Utility methods for the currently installed version of the Terra CLI. */
public class Version {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Version.class);
  /** Getter for the Terra CLI version of the current JAR. */
  public static String getVersion() {
    // read from the JAR Manifest file
    String version = Version.class.getPackage().getSpecificationVersion();
    if (version != null) {
      return version;
    }
    logger.warn(
        "Implementation version not defined in the JAR manifest. This is expected when testing, not during normal operation.");
    return System.getProperty("TERRA_CLI_VERSION");
  }
}
