package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.User;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a user for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link User} class for a user's internal representation.
 */
@JsonDeserialize(builder = UFUser.Builder.class)
public class UFUser implements UserFacingPrintable {
  public final String id;
  public final String email;
  public final String proxyGroupEmail;
  public final boolean loggedIn;

  /** Serialize an instance of the internal class to the disk format. */
  public UFUser(User internalObj) {
    this.id = internalObj.getId();
    this.email = internalObj.getEmail();
    this.proxyGroupEmail = internalObj.getProxyGroupEmail();
    this.loggedIn = internalObj.requiresReauthentication();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFUser(Builder builder) {
    this.id = builder.id;
    this.email = builder.email;
    this.proxyGroupEmail = builder.proxyGroupEmail;
    this.loggedIn = builder.loggedIn;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("User email: " + email);
    OUT.println("Proxy group email: " + proxyGroupEmail);
    OUT.println("LOGGED " + (loggedIn ? "IN" : "OUT"));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String id;
    private String email;
    private String proxyGroupEmail;
    private boolean loggedIn;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
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
    public UFUser build() {
      return new UFUser(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
