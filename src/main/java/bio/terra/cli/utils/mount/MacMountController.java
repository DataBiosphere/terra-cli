package bio.terra.cli.utils.mount;

import java.util.regex.Pattern;

public class MacMountController extends MountController {

  /**
   * Parses a mounted disk row outputted by `mount` into a regex pattern with the following 3
   * groups: 1. device name 2. mount point 3. mount options
   *
   * <p>An example of a mounted disk row is: genomics-public-data on
   * /Users/jupyter/workspace/genomics-public-data (macfuse, nodev, nosuid, synchronous, mounted by
   * jupyter)
   *
   * @return compiled regex pattern with 3 matcher groups
   */
  protected Pattern getMountEntryPattern() {
    return Pattern.compile("^(\\S+)\\s+on\\s+([^\\(]+)\\s+(\\([^\\)]++\\))");
  }
}
