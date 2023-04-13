package bio.terra.cli.utils.mount;

import java.util.regex.Pattern;

public class LinuxMountController extends MountController {

  protected Pattern getMountEntryPattern() {
    return Pattern.compile("^(\\S+)\\s+on\\s+([^\\(]+)\\s+(type[^\\)]++\\))");
  }
}
