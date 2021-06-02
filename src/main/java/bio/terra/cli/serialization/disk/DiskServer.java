package bio.terra.cli.serialization.disk;

import bio.terra.cli.Server;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = DiskServer.Builder.class)
public class DiskServer {
  public final String name;
  public final String description;
  public final String samUri;
  public final String workspaceManagerUri;
  public final String dataRepoUri;

  private DiskServer(DiskServer.Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.samUri = builder.samUri;
    this.workspaceManagerUri = builder.workspaceManagerUri;
    this.dataRepoUri = builder.dataRepoUri;
  }

  /** Builder class to construct an immutable Config object with lots of properties. */
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
    public DiskServer build() {
      return new DiskServer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(Server internalObj) {
      this.name = internalObj.getName();
      this.description = internalObj.getDescription();
      this.samUri = internalObj.getSamUri();
      this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
      this.dataRepoUri = internalObj.getDataRepoUri();
    }
  }
}
