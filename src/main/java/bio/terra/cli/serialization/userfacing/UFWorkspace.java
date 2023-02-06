package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * External representation of a workspace for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Workspace} class for a workspace's internal representation.
 *
 * <p>
 */
@JsonDeserialize(builder = UFWorkspace.Builder.class)
public class UFWorkspace extends UFWorkspaceLight {
  public final long numResources;

  /**
   * Serialize an instance of the internal class to the disk format. Note that the Workspace object
   * should have its resources populated in order for numResources to be correctly determined.
   */
  public UFWorkspace(Workspace internalObj) {
    super(internalObj);
    this.numResources = internalObj.listResources().size();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspace(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.cloudPlatform = builder.cloudPlatform;
    this.googleProjectId = builder.googleProjectId;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.properties = builder.properties;
    this.createdDate = builder.createdDate;
    this.lastUpdatedDate = builder.lastUpdatedDate;
    this.numResources = builder.numResources;
  }

  /** Print out a workspace object in text format. */
  @Override
  public void print() {
    super.print();
    PrintStream OUT = UserIO.getOut();
    // The space add for readable format because only used with terra workspace describe
    OUT.println("# Resources:       " + numResources);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String id;
    private String name;
    private String description;
    private CloudPlatform cloudPlatform;
    private String googleProjectId;
    private String serverName;
    private String userEmail;
    private Map<String, String> properties;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastUpdatedDate;
    private long numResources;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder id(String id) {
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

    public Builder cloudPlatform(CloudPlatform cloudPlatform) {
      this.cloudPlatform = cloudPlatform;
      return this;
    }

    public Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
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

    public Builder createdDate(OffsetDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder lastUpdatedDate(OffsetDateTime lastUpdatedDate) {
      this.lastUpdatedDate = lastUpdatedDate;
      return this;
    }

    public Builder numResources(long numResources) {
      this.numResources = numResources;
      return this;
    }

    /** Call the private constructor. */
    public UFWorkspace build() {
      return new UFWorkspace(this);
    }
  }
}
