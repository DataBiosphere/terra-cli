package bio.terra.cli.serialization.command;

import bio.terra.cli.Workspace;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonDeserialize(builder = CommandWorkspace.Builder.class)
public class CommandWorkspace {
  public final UUID id;
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String serverName;
  public final String userEmail;
  public final List<CommandResource> resources;

  private CommandWorkspace(CommandWorkspace.Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.resources = builder.resources;
  }

  /** Print out a workspace object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println("Terra workspace id: " + id);
    OUT.println("Display name: " + name);
    OUT.println("Description: " + description);
    OUT.println("Google project: " + googleProjectId);
    OUT.println(
        "Cloud console: https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID id;
    private String name;
    private String description;
    private String googleProjectId;
    private String serverName;
    private String userEmail;
    private List<CommandResource> resources;

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public Builder serverName(String serverName) {
      this.serverName = serverName;
      return this;
    }

    public Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public Builder resources(List<CommandResource> resources) {
      this.resources = resources;
      return this;
    }

    /** Call the private constructor. */
    public CommandWorkspace build() {
      return new CommandWorkspace(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(Workspace internalObj) {
      this.id = internalObj.getId();
      this.name = internalObj.getName();
      this.description = internalObj.getDescription();
      this.googleProjectId = internalObj.getGoogleProjectId();
      this.serverName = internalObj.getServerName();
      this.userEmail = internalObj.getUserEmail();
      this.resources =
          internalObj.getResources().stream()
              .map(resource -> resource.getResourceType().getCommandBuilder(resource).build())
              .collect(Collectors.toList());
    }
  }
}
