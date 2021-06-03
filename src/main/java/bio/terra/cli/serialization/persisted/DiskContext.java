package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.businessobject.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * External representation of the current context or state for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link Context} class for the current context's internal representation.
 */
@JsonDeserialize(builder = DiskContext.Builder.class)
public class DiskContext {
  public final DiskConfig config;
  public final DiskServer server;
  public final DiskUser user;
  public final DiskWorkspace workspace;

  /** Serialize an instance of the internal classes to the disk format. */
  public DiskContext(
      Config internalConfig,
      Server internalServer,
      @Nullable User internalUser,
      @Nullable Workspace internalWorkspace) {
    this.config = new DiskConfig(internalConfig);
    this.server = new DiskServer(internalServer);
    this.user = internalUser == null ? null : new DiskUser(internalUser);
    this.workspace = internalWorkspace == null ? null : new DiskWorkspace(internalWorkspace);
  }

  private DiskContext(DiskContext.Builder builder) {
    this.config = builder.config;
    this.server = builder.server;
    this.user = builder.user;
    this.workspace = builder.workspace;
  }

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
  }
}
