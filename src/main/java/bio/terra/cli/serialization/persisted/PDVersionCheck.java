package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.VersionCheck;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;

/**
 * External representation of version checking state for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link VersionCheck} class for a server's internal representation.
 */
public class PDVersionCheck {
  public final OffsetDateTime lastVersionCheckTime;

  /**
   * Default constructor for Jackson. Not sure why this class needs one and other PD classes don't.
   */
  public PDVersionCheck() {
    lastVersionCheckTime = null;
  }

  public PDVersionCheck(VersionCheck internalObj) {
    this.lastVersionCheckTime = internalObj.getLastVersionCheckTime();
  }

  private PDVersionCheck(PDVersionCheck.Builder builder) {
    this.lastVersionCheckTime = builder.lastVersionCheckTime;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private OffsetDateTime lastVersionCheckTime;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder lastVersionCheckTime(OffsetDateTime lastVersionCheckTime) {
      this.lastVersionCheckTime = lastVersionCheckTime;
      return this;
    }

    /** Call the private constructor. */
    public PDVersionCheck build() {
      return new PDVersionCheck(this);
    }
  }
}
