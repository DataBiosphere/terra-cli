package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.Properties;
import bio.terra.workspace.model.Property;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/** This is used instead of UFWorkspace, if workspace resources aren't needed. */
public class UFWorkspaceLight {
  // "id" instead of "userFacingId" because user sees this with "terra workspace describe
  // --format=json"
  public String id;
  public String name;
  public String description;
  public CloudPlatform cloudPlatform;
  public String googleProjectId;
  public Map<String, String> properties;
  public String serverName;
  public String userEmail;
  public OffsetDateTime createdDate;
  public OffsetDateTime lastUpdatedDate;

  /**
   * It's expected that the workspace passed into this constructor does not have its resources
   * populated. If it does, then one should create a UFWorkspace instead.
   *
   * @param internalObj
   */
  public UFWorkspaceLight(Workspace internalObj) {
    this.id = internalObj.getUserFacingId();
    this.googleProjectId = internalObj.getGoogleProjectId().orElse(null);
    this.cloudPlatform = internalObj.getCloudPlatform();

    WorkspaceDescription workspaceDescription = internalObj.getWorkspaceDescription();
    this.name = workspaceDescription.getDisplayName();
    this.description = workspaceDescription.getDescription();
    this.properties = propertiesToStringMap(workspaceDescription.getProperties());
    this.userEmail = Context.requireUser().getEmail();
    this.serverName = Context.getServer().getName();
    this.createdDate = workspaceDescription.getCreatedDate();
    this.lastUpdatedDate = workspaceDescription.getLastUpdatedDate();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspaceLight(UFWorkspaceLight.Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.cloudPlatform = builder.cloudPlatform;
    this.googleProjectId = builder.googleProjectId;
    this.properties = builder.properties;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.createdDate = builder.createdDate;
    this.lastUpdatedDate = builder.lastUpdatedDate;
  }

  /** Default constructor for subclass Builder constructor */
  protected UFWorkspaceLight() {
    this.id = null;
    this.name = null;
    this.description = null;
    this.cloudPlatform = null;
    this.googleProjectId = null;
    this.properties = null;
    this.serverName = null;
    this.userEmail = null;
    this.createdDate = null;
    this.lastUpdatedDate = null;
  }

  /** Print out a workspace object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    // "id" instead of "userFacingId" because user sees this with "terra workspace describe
    // --format=json"
    OUT.println("ID:                " + id);
    OUT.println("Name:              " + name);
    OUT.println("Description:       " + description);
    if (cloudPlatform == CloudPlatform.GCP) {
      OUT.println("Google project:    " + googleProjectId);
      OUT.println(
          "Cloud console:     https://console.cloud.google.com/home/dashboard?project="
              + googleProjectId);
    }
    if (properties != null) {
      OUT.println("Properties:");
      properties.forEach((key, value) -> OUT.println("  " + key + ": " + value));
    }
    OUT.println("Created:           " + createdDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    OUT.println("Last updated:      " + lastUpdatedDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    // "id" instead of "userFacingId" because user sees this with "terra workspace describe
    // --format=json"
    private String id;
    private String name;
    private String description;
    private CloudPlatform cloudPlatform;
    private String googleProjectId;
    private Map<String, String> properties;
    private String serverName;
    private String userEmail;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastUpdatedDate;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UFWorkspaceLight.Builder id(String id) {
      this.id = id;
      return this;
    }

    public UFWorkspaceLight.Builder name(String name) {
      this.name = name;
      return this;
    }

    public UFWorkspaceLight.Builder description(String description) {
      this.description = description;
      return this;
    }

    public UFWorkspaceLight.Builder cloudPlatform(CloudPlatform cloudPlatform) {
      this.cloudPlatform = cloudPlatform;
      return this;
    }

    public UFWorkspaceLight.Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public UFWorkspaceLight.Builder properties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public UFWorkspaceLight.Builder serverName(String serverName) {
      this.serverName = serverName;
      return this;
    }

    public UFWorkspaceLight.Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public UFWorkspaceLight.Builder createdDate(OffsetDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public UFWorkspaceLight.Builder lastUpdatedDate(OffsetDateTime lastUpdatedDate) {
      this.lastUpdatedDate = lastUpdatedDate;
      return this;
    }

    /** Call the private constructor. */
    public UFWorkspaceLight build() {
      return new UFWorkspaceLight(this);
    }
  }

  private static Map<String, String> propertiesToStringMap(Properties properties) {
    return properties.stream().collect(Collectors.toMap(Property::getKey, Property::getValue));
  }
}
