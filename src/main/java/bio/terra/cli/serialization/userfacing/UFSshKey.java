package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

@JsonDeserialize(builder = UFSshKey.Builder.class)
public class UFSshKey {
  public final String privateSshKey;
  public final String publicSshKey;
  public final String userEmail;

  private UFSshKey(Builder builder) {
    this.privateSshKey = builder.privateSshKey;
    this.publicSshKey = builder.publicSshKey;
    this.userEmail = builder.userEmail;
  }

  public static UFSshKey createUFSshKey(String privateKey, String publicKey, String userEmail) {
    return new Builder()
        .privateSshKey(privateKey)
        .publicSshKey(publicKey)
        .userEmail(userEmail)
        .build();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("User email: " + userEmail);
    OUT.println("Private key: ");
    OUT.println(privateSshKey);
    OUT.println("Public key: ");
    OUT.println(publicSshKey);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String privateSshKey;
    private String publicSshKey;
    private String userEmail;

    public Builder privateSshKey(String privateSshKey) {
      this.privateSshKey = privateSshKey;
      return this;
    }

    public Builder publicSshKey(String publicSshKey) {
      this.publicSshKey = publicSshKey;
      return this;
    }

    private Builder userEmail(String email) {
      userEmail = email;
      return this;
    }

    /** Call the private constructor. */
    public UFSshKey build() {
      return new UFSshKey(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
