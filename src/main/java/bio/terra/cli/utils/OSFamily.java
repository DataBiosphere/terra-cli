package bio.terra.cli.utils;

/** This class provites a method to get the OS family. */
public class OSFamily {
  public static final String MAC = "mac";
  public static final String LINUX = "linux";
  public static final String WINDOWS = "windows";
  public static final String UNKNOWN = "unknown";

  /**
   * Returns the OS family.
   *
   * @return the OS family string
   */
  public static String getOSFamily() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return MAC;
    } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
      return LINUX;
    } else if (os.contains("win")) {
      return WINDOWS;
    } else {
      return UNKNOWN;
    }
  }
}
