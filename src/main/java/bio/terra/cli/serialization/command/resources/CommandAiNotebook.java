package bio.terra.cli.serialization.command.resources;

import bio.terra.cli.businessobject.resources.AiNotebook;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.notebooks.v1.model.Instance;
import java.io.PrintStream;

/**
 * External representation of a workspace AI notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AiNotebook} class for a notebook's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CommandAiNotebook.Builder.class)
public class CommandAiNotebook extends CommandResource {
  public final String projectId;
  public final String instanceId;
  public final String location;
  public final String instanceName;
  public final String state;
  public final String proxyUri;
  public final String createTime;

  /** Serialize an instance of the internal class to the command format. */
  public CommandAiNotebook(AiNotebook internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();

    Instance instance = internalObj.getInstance();
    this.instanceName = instance.getName();
    this.state = instance.getState();
    this.proxyUri = instance.getProxyUri();
    this.createTime = instance.getCreateTime();
  }

  /** Constructor for Jackson deserialization during testing. */
  private CommandAiNotebook(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
    this.instanceName = builder.instanceName;
    this.state = builder.state;
    this.proxyUri = builder.proxyUri;
    this.createTime = builder.createTime;
  }

  /** Print out this object in text format. */
  public void print() {
    super.print();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id:                " + projectId);
    OUT.println("AI Notebook instance location: " + location);
    OUT.println("AI Notebook instance id:       " + instanceId);
    OUT.println("Instance name: " + instanceName);
    OUT.println("State:         " + state);
    OUT.println("Proxy URL:     " + proxyUri);
    OUT.println("Create time:   " + createTime);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends CommandResource.Builder {
    private String projectId;
    private String instanceId;
    private String location;
    private String instanceName;
    private String state;
    private String proxyUri;
    private String createTime;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder instanceName(String instanceName) {
      this.instanceName = instanceName;
      return this;
    }

    public Builder state(String state) {
      this.state = state;
      return this;
    }

    public Builder proxyUri(String proxyUri) {
      this.proxyUri = proxyUri;
      return this;
    }

    public Builder createTime(String createTime) {
      this.createTime = createTime;
      return this;
    }

    /** Call the private constructor. */
    public CommandAiNotebook build() {
      return new CommandAiNotebook(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
