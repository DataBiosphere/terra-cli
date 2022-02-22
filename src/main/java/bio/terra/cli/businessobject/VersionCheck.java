package bio.terra.cli.businessobject;

import bio.terra.cli.serialization.persisted.PDVersionCheck;
import java.time.OffsetDateTime;

/**
 * The obsolete version check requires storing state in the context file, which corresponds to this
 * business object.
 */
public class VersionCheck {
  private final OffsetDateTime lastVersionCheckTime;

  public VersionCheck(OffsetDateTime lastVersionCheckTime) {
    this.lastVersionCheckTime = lastVersionCheckTime;
  }

  public VersionCheck(PDVersionCheck configFromDisk) {
    this.lastVersionCheckTime = configFromDisk.lastVersionCheckTime;
  }

  public OffsetDateTime getLastVersionCheckTime() {
    return lastVersionCheckTime;
  }
}
