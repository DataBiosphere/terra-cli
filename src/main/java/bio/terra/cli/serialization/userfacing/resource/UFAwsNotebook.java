package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.AwsSageMakerProxyUrlView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

/**
 * External representation of a workspace AWS notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsNotebook} class for a notebook's internal representation.
 */
@JsonDeserialize(builder = UFAwsNotebook.Builder.class)
public class UFAwsNotebook extends UFResource {
  public final String instanceId;
  public final String location;
  public final String instanceName;
  public final String state;
  public final String proxyUriJupyter;
  public final String proxyUriJupyterLab;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsNotebook(AwsNotebook internalObj) {
    super(internalObj);
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();
    this.instanceName = AwsNotebook.resolve(location, instanceId, true);
    this.state =
        AwsNotebook.getStatus(location, instanceId)
            .orElse(NotebookInstanceStatus.UNKNOWN_TO_SDK_VERSION)
            .toString();
    this.proxyUriJupyter =
        AwsNotebook.getProxyUri(instanceId, AwsSageMakerProxyUrlView.JUPYTER).orElse(null);
    this.proxyUriJupyterLab =
        AwsNotebook.getProxyUri(instanceId, AwsSageMakerProxyUrlView.JUPYTERLAB).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsNotebook(Builder builder) {
    super(builder);
    this.instanceId = builder.instanceId;
    this.location = builder.location;
    this.instanceName = builder.instanceName;
    this.state = builder.state;
    this.proxyUriJupyter = builder.proxyUriJupyter;
    this.proxyUriJupyterLab = builder.proxyUriJupyterLab;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Instance id:   " + instanceId);
    OUT.println(prefix + "Location:      " + (location == null ? "(undefined)" : location));
    OUT.println(prefix + "Instance name: " + instanceName);
    OUT.println(prefix + "State:         " + state);
    OUT.println(
        prefix
            + "ProxyUri (JUPYTER):    "
            + (proxyUriJupyter == null ? "(unavailable)" : proxyUriJupyter));
    OUT.println(
        prefix
            + "ProxyUri (JUPYTERLAB): "
            + (proxyUriJupyter == null ? "(unavailable)" : proxyUriJupyter));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String instanceId;
    private String location;
    private String instanceName;
    private String state;
    private String proxyUriJupyter;
    private String proxyUriJupyterLab;

    /** Default constructor for Jackson. */
    public Builder() {}

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

    public Builder proxyUriJupyter(String proxyUriJupyter) {
      this.proxyUriJupyter = proxyUriJupyter;
      return this;
    }

    public Builder proxyUriJupyterLab(String proxyUriJupyterLab) {
      this.proxyUriJupyterLab = proxyUriJupyterLab;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsNotebook build() {
      return new UFAwsNotebook(this);
    }
  }
}
