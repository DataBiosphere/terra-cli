package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcpNotebook;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.notebooks.v1.model.Instance;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace GCP notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcpNotebook} class for a notebook's internal representation.
 */
@JsonDeserialize(builder = UFGcpNotebook.Builder.class)
public class UFGcpNotebook extends UFResource {
  public final String projectId;
  public final String instanceId;
  public final String location;
  public final String instanceName;
  public final String state;
  public final String proxyUri;
  public final String createTime;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcpNotebook(GcpNotebook internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();

    Optional<Instance> instance = internalObj.getInstance();
    this.instanceName = instance.isPresent() ? instance.get().getName() : null;
    this.state = instance.isPresent() ? instance.get().getState() : null;
    this.proxyUri = instance.isPresent() ? instance.get().getProxyUri() : null;
    this.createTime = instance.isPresent() ? instance.get().getCreateTime() : null;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcpNotebook(Builder builder) {
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
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCP project id:                " + projectId);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
    OUT.println(prefix + "Instance id:       " + instanceId);
    OUT.println(prefix + "Instance name: " + (instanceName == null ? "(undefined)" : instanceName));
    OUT.println(prefix + "State:         " + (state == null ? "(undefined)" : state));
    OUT.println(prefix + "Proxy URL:     " + (proxyUri == null ? "(undefined)" : proxyUri));
    OUT.println(prefix + "Create time:   " + (createTime == null ? "(undefined)" : createTime));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
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
    public UFGcpNotebook build() {
      return new UFGcpNotebook(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
