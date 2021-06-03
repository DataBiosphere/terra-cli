package bio.terra.cli.serialization.command;

import bio.terra.cli.Server;
import bio.terra.cli.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of the current server & workspace status for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = CommandStatus.Builder.class)
public class CommandStatus {
  // global server context = service uris, environment name
  public final CommandServer server;

  // global workspace context
  public final CommandWorkspace workspace;

  public CommandStatus(Server server, Workspace workspace) {
    this.server = new CommandServer(server);
    this.workspace = workspace != null ? new CommandWorkspace(workspace) : null;
  }

  /** Constructor for Jackson deserialization during testing. */
  private CommandStatus(Builder builder) {
    this.server = builder.server;
    this.workspace = builder.workspace;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CommandServer server;
    private CommandWorkspace workspace;

    public Builder server(CommandServer server) {
      this.server = server;
      return this;
    }

    public Builder workspace(CommandWorkspace workspace) {
      this.workspace = workspace;
      return this;
    }

    /** Call the private constructor. */
    public CommandStatus build() {
      return new CommandStatus(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
