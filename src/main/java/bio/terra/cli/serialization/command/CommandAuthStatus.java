package bio.terra.cli.serialization.command;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the user status for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = CommandAuthStatus.Builder.class)
public class CommandAuthStatus {
  // Terra user email associated with the current user
  public final String userEmail;

  // Terra proxy group email associated with the current user
  public final String proxyGroupEmail;

  // true if the current user does not need to re-authenticate
  public final boolean loggedIn;

  /** Constructor for Jackson deserialization during testing. */
  private CommandAuthStatus(Builder builder) {
    this.userEmail = builder.userEmail;
    this.proxyGroupEmail = builder.proxyGroupEmail;
    this.loggedIn = builder.loggedIn;
  }

  /** Constructor for when there is a current user defined. */
  public static CommandAuthStatus createWhenCurrentUserIsDefined(
      String userEmail, String proxyGroupEmail, boolean loggedIn) {
    return new Builder()
        .userEmail(userEmail)
        .proxyGroupEmail(proxyGroupEmail)
        .loggedIn(loggedIn)
        .build();
  }

  /** Constructor for when there is NOT a current user defined. */
  public static CommandAuthStatus createWhenCurrentUserIsUndefined() {
    return new Builder().loggedIn(false).build();
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String userEmail;
    private String proxyGroupEmail;
    private boolean loggedIn;

    public Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public Builder proxyGroupEmail(String proxyGroupEmail) {
      this.proxyGroupEmail = proxyGroupEmail;
      return this;
    }

    public Builder loggedIn(boolean loggedIn) {
      this.loggedIn = loggedIn;
      return this;
    }

    /** Call the private constructor. */
    public CommandAuthStatus build() {
      return new CommandAuthStatus(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
