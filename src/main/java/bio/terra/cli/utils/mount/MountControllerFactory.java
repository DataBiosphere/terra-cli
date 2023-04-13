package bio.terra.cli.utils.mount;

public class MountControllerFactory {
  public static MountController getMountController() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return new MacMountController();
    } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
      return new LinuxMountController();
    } else {
      throw new UnsupportedOperationException("Unsupported operating system.");
    }
  }
}
