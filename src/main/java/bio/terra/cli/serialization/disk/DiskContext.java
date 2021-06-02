package bio.terra.cli.serialization.disk;

import bio.terra.cli.Config;
import bio.terra.cli.Server;
import bio.terra.cli.User;
import bio.terra.cli.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

@JsonDeserialize(builder = DiskContext.Builder.class)
public class DiskContext {
  public final DiskConfig config;
  public final DiskServer server;
  public final DiskUser user;
  public final DiskWorkspace workspace;

  private DiskContext(DiskContext.Builder builder) {
    this.config = builder.config;
    this.server = builder.server;
    this.user = builder.user;
    this.workspace = builder.workspace;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private DiskConfig config;
    private DiskServer server;
    private DiskUser user;
    private DiskWorkspace workspace;

    public Builder config(DiskConfig config) {
      this.config = config;
      return this;
    }

    public Builder server(DiskServer server) {
      this.server = server;
      return this;
    }

    public Builder user(DiskUser user) {
      this.user = user;
      return this;
    }

    public Builder workspace(DiskWorkspace workspace) {
      this.workspace = workspace;
      return this;
    }

    /** Call the private constructor. */
    public DiskContext build() {
      return new DiskContext(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal classes to the disk format. */
    public Builder(
        Config internalConfig,
        Server internalServer,
        @Nullable User internalUser,
        @Nullable Workspace internalWorkspace) {
      this.config = new DiskConfig.Builder(internalConfig).build();
      this.server = new DiskServer.Builder(internalServer).build();
      this.user = internalUser == null ? null : new DiskUser.Builder(internalUser).build();
      this.workspace =
          internalWorkspace == null ? null : new DiskWorkspace.Builder(internalWorkspace).build();
    }
  }
}
