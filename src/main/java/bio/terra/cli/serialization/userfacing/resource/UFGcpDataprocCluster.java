package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.dataproc.ClusterName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Map;

/**
 * External representation of a workspace GCP Dataproc cluster resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcpDataprocCluster} class for a Dataproc cluster's internal representation.
 */
@JsonDeserialize(builder = UFGcpDataprocCluster.Builder.class)
public class UFGcpDataprocCluster extends UFResource {
  public final ClusterName clusterName;
  // public final String state;
  // public final Map<String, String> metadata;
  // public final String proxyUri;
  // public final String createTime;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcpDataprocCluster(GcpDataprocCluster internalObj) {
    super(internalObj);
    this.clusterName = internalObj.getClusterName();

    // Optional<Instance> instance = internalObj.getInstance();
    // this.state = instance.map(Instance::getState).orElse(null);
    // this.metadata = instance.map(Instance::getMetadata).orElse(null);
    // this.proxyUri = instance.map(Instance::getProxyUri).orElse(null);
    // this.createTime = instance.map(Instance::getCreateTime).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcpDataprocCluster(Builder builder) {
    super(builder);
    this.clusterName = builder.clusterName;
    // this.state = builder.state;
    // this.metadata = builder.metadata;
    // this.proxyUri = builder.proxyUri;
    // this.createTime = builder.createTime;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Cluster Name: " + clusterName.name());
    // OUT.println(prefix + "State:         " + (state == null ? "(undefined)" : state));
    // if (metadata != null) {
    //   OUT.println(prefix + "Metadata:");
    //   metadata.forEach((key, value) -> OUT.println("   " + key + ": " + value));
    // } else {
    //   OUT.println(prefix + "Metadata:      (undefined)");
    // }
    // OUT.println(prefix + "Proxy URL:     " + (proxyUri == null ? "(undefined)" : proxyUri));
    // OUT.println(prefix + "Create time:   " + (createTime == null ? "(undefined)" : createTime));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private ClusterName clusterName;
    private String state;
    private Map<String, String> metadata;
    private String proxyUri;
    private String createTime;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder instanceName(ClusterName clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder state(String state) {
      this.state = state;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
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
    public UFGcpDataprocCluster build() {
      return new UFGcpDataprocCluster(this);
    }
  }
}
