package bio.terra.cli.utils.mount;

import java.util.regex.Pattern;

public class LinuxMountController extends MountController {

  /**
   * Parses a mounted disk row outputted by `mount` into a regex pattern with the following 3
   * groups: 1. device name 2. mount point 3. mount options
   *
   * <p>An example of a mounted disk row is: genomics-public-data on /home/jupyter/workspace type
   * fuse.gcsfuse (rw,nosuid,nodev,relatime,user_id=1000,group_id=1001,default_permissions)
   *
   * @return compiled regex pattern with 3 matcher groups
   */
  protected Pattern getMountEntryPattern() {
    return Pattern.compile("^(\\S+)\\s+on\\s+([^\\(]+)\\s+(type[^\\)]+\\))");
  }
}
