package bio.terra.cli.context.utils;

/** Utility methods for the currently installed version of the Terra CLI. */
public class Version {
  /** Getter for the Terra CLI version of the current JAR. */
  public static String getVersion() {
    // read from the JAR Manifest file
    return Version.class.getPackage().getImplementationVersion();
  }
}
