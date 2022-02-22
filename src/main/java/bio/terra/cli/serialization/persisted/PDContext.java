package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.businessobject.VersionCheck;
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
@JsonDeserialize(builder = PDContext.Builder.class)
public class PDContext {
  public final PDConfig config;
  public final PDServer server;
  public final PDUser user;
  public final PDWorkspace workspace;
  public final PDVersionCheck versionCheck;

  /** Serialize an instance of the internal classes to the disk format. */
  public PDContext(
      Config internalConfig,
      Server internalServer,
      @Nullable User internalUser,
      @Nullable Workspace internalWorkspace,
      @Nullable VersionCheck versionCheck) {
    this.config = new PDConfig(internalConfig);
    this.server = new PDServer(internalServer);
    this.user = internalUser == null ? null : new PDUser(internalUser);
    this.workspace = internalWorkspace == null ? null : new PDWorkspace(internalWorkspace);
    this.versionCheck = versionCheck == null ? null : new PDVersionCheck(versionCheck);
  }

  private PDContext(PDContext.Builder builder) {
    this.config = builder.config;
    this.server = builder.server;
    this.user = builder.user;
    this.workspace = builder.workspace;
    this.versionCheck = builder.versionCheck;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private PDConfig config;
    private PDServer server;
    private PDUser user;
    private PDWorkspace workspace;
    private PDVersionCheck versionCheck;

    public Builder config(PDConfig config) {
      this.config = config;
      return this;
    }

    public Builder server(PDServer server) {
      this.server = server;
      return this;
    }

    public Builder user(PDUser user) {
      this.user = user;
      return this;
    }

    public Builder workspace(PDWorkspace workspace) {
      this.workspace = workspace;
      return this;
    }

    public Builder versionCheck(PDVersionCheck versionCheck) {
      this.versionCheck = versionCheck;
      return this;
    }

    /** Call the private constructor. */
    public PDContext build() {
      return new PDContext(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
