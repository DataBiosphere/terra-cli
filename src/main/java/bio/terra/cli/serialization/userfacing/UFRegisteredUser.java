package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.RegisteredUser;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a registered user for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link RegisteredUser} class for a registered user's internal representation.
 */
@JsonDeserialize(builder = UFRegisteredUser.Builder.class)
public class UFRegisteredUser {
  public final String email;
  public final String subjectId;
  public final boolean isRegistered;
  public final boolean isEnabled;

  /** Serialize an instance of the internal class to the disk format. */
  public UFRegisteredUser(RegisteredUser internalObj) {
    this.email = internalObj.getEmail();
    this.subjectId = internalObj.getSubjectId();
    this.isRegistered = internalObj.isRegistered();
    this.isEnabled = internalObj.isEnabled();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFRegisteredUser(Builder builder) {
    this.email = builder.email;
    this.subjectId = builder.subjectId;
    this.isRegistered = builder.isRegistered;
    this.isEnabled = builder.isEnabled;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("Email: " + email);
    OUT.println("Subject ID: " + subjectId);
    OUT.println((isRegistered ? "" : "NOT ") + "REGISTERED");
    OUT.println(isEnabled ? "ENABLED" : "DISABLED");
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String email;
    private String subjectId;
    private boolean isRegistered;
    private boolean isEnabled;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder subjectId(String subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    public Builder isRegistered(boolean isRegistered) {
      this.isRegistered = isRegistered;
      return this;
    }

    public Builder isEnabled(boolean isEnabled) {
      this.isEnabled = isEnabled;
      return this;
    }

    /** Call the private constructor. */
    public UFRegisteredUser build() {
      return new UFRegisteredUser(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
