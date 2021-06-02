package bio.terra.cli.serialization.disk;

import bio.terra.cli.User;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DiskUser.Builder.class)
public class DiskUser {
  public final String id;
  public final String email;
  public final String proxyGroupEmail;

  private DiskUser(DiskUser.Builder builder) {
    this.id = builder.id;
    this.email = builder.email;
    this.proxyGroupEmail = builder.proxyGroupEmail;
  }

  /** Builder class to construct an immutable object with lots of properties. */
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
    public DiskUser build() {
      return new DiskUser(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(User internalObj) {
      this.id = internalObj.getId();
      this.email = internalObj.getEmail();
      this.proxyGroupEmail = internalObj.getProxyGroupEmail();
    }
  }
}
