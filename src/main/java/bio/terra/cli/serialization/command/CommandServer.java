package bio.terra.cli.serialization.command;

import bio.terra.cli.Server;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

@JsonDeserialize(builder = CommandServer.Builder.class)
public class CommandServer {
  public final String name;
  public final String description;
  public final String samUri;
  public final String workspaceManagerUri;
  public final String dataRepoUri;

  private CommandServer(CommandServer.Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.samUri = builder.samUri;
    this.workspaceManagerUri = builder.workspaceManagerUri;
    this.dataRepoUri = builder.dataRepoUri;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    OUT.println(name + ": " + description);
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String description;
    private String samUri;
    private String workspaceManagerUri;
    private String dataRepoUri;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder samUri(String samUri) {
      this.samUri = samUri;
      return this;
    }

    public Builder workspaceManagerUri(String workspaceManagerUri) {
      this.workspaceManagerUri = workspaceManagerUri;
      return this;
    }

    public Builder dataRepoUri(String dataRepoUri) {
      this.dataRepoUri = dataRepoUri;
      return this;
    }

    /** Call the private constructor. */
    public CommandServer build() {
      return new CommandServer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the command format. */
    public Builder(Server internalObj) {
      this.name = internalObj.getName();
      this.description = internalObj.getDescription();
      this.samUri = internalObj.getSamUri();
      this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
      this.dataRepoUri = internalObj.getDataRepoUri();
    }
  }
}
