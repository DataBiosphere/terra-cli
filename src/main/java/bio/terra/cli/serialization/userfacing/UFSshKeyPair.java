package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.UserIO;
import bio.terra.externalcreds.model.SshKeyPair;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

@JsonDeserialize(builder = UFSshKeyPair.Builder.class)
public class UFSshKeyPair {
  public final String privateSshKey;
  public final String publicSshKey;
  public final String userEmail;

  private UFSshKeyPair(Builder builder) {
    this.privateSshKey = builder.privateSshKey;
    this.publicSshKey = builder.publicSshKey;
    this.userEmail = builder.userEmail;
  }

  public static UFSshKeyPair createUFSshKey(SshKeyPair sshKeyPair) {
    return new Builder()
        .privateSshKey(sshKeyPair.getPrivateKey())
        .publicSshKey(sshKeyPair.getPublicKey())
        .userEmail(sshKeyPair.getExternalUserEmail())
        .build();
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println(publicSshKey);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String privateSshKey;
    private String publicSshKey;
    private String userEmail;

    /** Default constructor for Jackson. */
    public Builder() {}

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
    public UFSshKeyPair build() {
      return new UFSshKeyPair(this);
    }
  }
}
