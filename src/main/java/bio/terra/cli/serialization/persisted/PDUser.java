package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.User;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a user for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link User} class for a user's internal representation.
 */
@JsonDeserialize(builder = PDUser.Builder.class)
public class PDUser {
  public final String id;
  public final String email;
  public final String proxyGroupEmail;

  /** Serialize an instance of the internal class to the disk format. */
  public PDUser(User internalObj) {
    this.id = internalObj.getId();
    this.email = internalObj.getEmail();
    this.proxyGroupEmail = internalObj.getProxyGroupEmail();
  }

  private PDUser(PDUser.Builder builder) {
    this.id = builder.id;
    this.email = builder.email;
    this.proxyGroupEmail = builder.proxyGroupEmail;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String id;
    private String email;
    private String proxyGroupEmail;

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

    /** Call the private constructor. */
    public PDUser build() {
      return new PDUser(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
