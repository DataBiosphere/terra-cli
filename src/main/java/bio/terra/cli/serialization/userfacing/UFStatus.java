package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the current server & workspace status for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFStatus.Builder.class)
public class UFStatus {
  // global server context = service uris, environment name
  public final UFServer server;

  // global workspace context
  public final UFWorkspace workspace;

  public UFStatus(Server server, Workspace workspace) {
    this.server = new UFServer(server);
    this.workspace = workspace != null ? new UFWorkspace(workspace) : null;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFStatus(Builder builder) {
    this.server = builder.server;
    this.workspace = builder.workspace;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UFServer server;
    private UFWorkspace workspace;

    public Builder server(UFServer server) {
      this.server = server;
      return this;
    }

    public Builder workspace(UFWorkspace workspace) {
      this.workspace = workspace;
      return this;
    }

    /** Call the private constructor. */
    public UFStatus build() {
      return new UFStatus(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
