package bio.terra.cli.utils;

/** Utility methods for the currently installed version of the Terra CLI. */
public class Version {
  /** Getter for the Terra CLI version of the current JAR. */
  public static String getVersion() {
    // read from the JAR Manifest file
    String version = Version.class.getPackage().getSpecificationVersion();
    if (version != null) {
      return version;
    }
    return System.getProperty("TERRA_VERSION");
  }
}
