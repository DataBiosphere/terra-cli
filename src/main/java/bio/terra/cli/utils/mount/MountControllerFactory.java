package bio.terra.cli.utils.mount;

import bio.terra.cli.utils.OSFamily;

public class MountControllerFactory {
  public static MountController getMountController() {
    String os = OSFamily.getOSFamily();
    if (os.equals(OSFamily.MAC)) {
      return new MacMountController();
    } else if (os.equals(OSFamily.LINUX)) {
      return new LinuxMountController();
    } else {
      throw new UnsupportedOperationException("Unsupported operating system.");
    }
  }
}
