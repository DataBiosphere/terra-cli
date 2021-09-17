package bio.terra.cli.serialization.userfacing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the user status for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFAuthStatus.Builder.class)
public class UFAuthStatus implements UserFacing {
  // Terra user email associated with the current user
  public final String userEmail;

  // Terra proxy group email associated with the current user
  public final String proxyGroupEmail;

  // Terra pet SA email associated with the current user + workspace
  public final String serviceAccountEmail;

  // true if the current user does not need to re-authenticate
  public final boolean loggedIn;

  /** Constructor for Jackson deserialization during testing. */
  private UFAuthStatus(Builder builder) {
    this.userEmail = builder.userEmail;
    this.proxyGroupEmail = builder.proxyGroupEmail;
    this.serviceAccountEmail = builder.serviceAccountEmail;
    this.loggedIn = builder.loggedIn;
  }

  /** Constructor for when there is a current user defined. */
  public static UFAuthStatus createWhenCurrentUserIsDefined(
      String userEmail, String proxyGroupEmail, String serviceAccountEmail, boolean loggedIn) {
    return new Builder()
        .userEmail(userEmail)
        .proxyGroupEmail(proxyGroupEmail)
        .serviceAccountEmail(serviceAccountEmail)
        .loggedIn(loggedIn)
        .build();
  }

  /** Constructor for when there is NOT a current user defined. */
  public static UFAuthStatus createWhenCurrentUserIsUndefined() {
    return new Builder().loggedIn(false).build();
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String userEmail;
    private String proxyGroupEmail;
    private String serviceAccountEmail;
    private boolean loggedIn;

    public Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public Builder proxyGroupEmail(String proxyGroupEmail) {
      this.proxyGroupEmail = proxyGroupEmail;
      return this;
    }

    public Builder serviceAccountEmail(String serviceAccountEmail) {
      this.serviceAccountEmail = serviceAccountEmail;
      return this;
    }

    public Builder loggedIn(boolean loggedIn) {
      this.loggedIn = loggedIn;
      return this;
    }

    /** Call the private constructor. */
    public UFAuthStatus build() {
      return new UFAuthStatus(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
