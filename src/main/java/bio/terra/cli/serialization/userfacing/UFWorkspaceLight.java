package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.PropertiesUtils;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.AwsContext;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.WorkspaceDescription;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/** This is used instead of UFWorkspace, if workspace resources aren't needed. */
public class UFWorkspaceLight {
  // "id" instead of "userFacingId" because user sees this with "terra workspace describe
  // --format=json"
  public String id;
  public UUID uuid;
  public CloudPlatform cloudPlatform;

  // GCP
  public String googleProjectId;

  // AWS
  public String awsMajorVersion;
  public String awsOrganizationId;
  public String awsAccountId;
  public String awsTenantAlias;
  public String awsEnvironmentAlias;

  public String name;
  public String description;
  public Map<String, String> properties;
  public String serverName;
  public String userEmail;
  public OffsetDateTime createdDate;
  public OffsetDateTime lastUpdatedDate;

  /**
   * It's expected that the workspace passed into this constructor does not have its resources
   * populated. If it does, then one should create a UFWorkspace instead.
   *
   * @param internalObj Workspace
   */
  public UFWorkspaceLight(Workspace internalObj) {
    this.id = internalObj.getUserFacingId();
    this.uuid = internalObj.getUuid();
    this.cloudPlatform = internalObj.getCloudPlatform();
    this.googleProjectId = internalObj.getGoogleProjectId().orElse(null);
    this.awsMajorVersion = internalObj.getAwsMajorVersion().orElse(null);
    this.awsOrganizationId = internalObj.getAwsOrganizationId().orElse(null);
    this.awsAccountId = internalObj.getAwsAccountId().orElse(null);
    this.awsTenantAlias = internalObj.getAwsTenantAlias().orElse(null);
    this.awsEnvironmentAlias = internalObj.getAwsEnvironmentAlias().orElse(null);

    WorkspaceDescription workspaceDescription = internalObj.getWorkspaceDescription();
    this.name = workspaceDescription.getDisplayName();
    this.description = workspaceDescription.getDescription();
    this.properties = PropertiesUtils.propertiesToStringMap(workspaceDescription.getProperties());
    this.userEmail = Context.requireUser().getEmail();
    this.serverName = Context.getServer().getName();
    this.createdDate = workspaceDescription.getCreatedDate();
    this.lastUpdatedDate = workspaceDescription.getLastUpdatedDate();
  }

  /**
   * This constructor can be used instead of {@link UFWorkspaceLight#UFWorkspaceLight(Workspace)} if
   * you already have a WorkspaceDescription object from WSM to avoid making additional calls.
   *
   * @param workspaceDescription workspaceDescription
   */
  public UFWorkspaceLight(WorkspaceDescription workspaceDescription) {
    this.id = workspaceDescription.getUserFacingId();
    this.uuid = workspaceDescription.getId();

    if (workspaceDescription.getGcpContext() != null) {
      this.cloudPlatform = CloudPlatform.GCP;
      this.googleProjectId = workspaceDescription.getGcpContext().getProjectId();

    } else if (workspaceDescription.getAzureContext() != null) {
      this.cloudPlatform = CloudPlatform.AZURE;

    } else if (workspaceDescription.getAwsContext() != null) {
      cloudPlatform = CloudPlatform.AWS;

      AwsContext awsContext = workspaceDescription.getAwsContext();
      awsMajorVersion = awsContext.getMajorVersion();
      awsOrganizationId = awsContext.getOrganizationId();
      awsAccountId = awsContext.getAccountId();
      awsTenantAlias = awsContext.getTenantAlias();
      awsEnvironmentAlias = awsContext.getEnvironmentAlias();
    }

    this.name = workspaceDescription.getDisplayName();
    this.description = workspaceDescription.getDescription();
    this.properties = PropertiesUtils.propertiesToStringMap(workspaceDescription.getProperties());
    this.userEmail = Context.requireUser().getEmail();
    this.serverName = Context.getServer().getName();
    this.createdDate = workspaceDescription.getCreatedDate();
    this.lastUpdatedDate = workspaceDescription.getLastUpdatedDate();
  }

  /** Constructor for Jackson deserialization during testing. */
  protected UFWorkspaceLight(Builder builder) {
    this.id = builder.id;
    this.uuid = builder.uuid;
    this.cloudPlatform = builder.cloudPlatform;
    this.googleProjectId = builder.googleProjectId;
    this.awsMajorVersion = builder.awsMajorVersion;
    this.awsOrganizationId = builder.awsOrganizationId;
    this.awsAccountId = builder.awsAccountId;
    this.awsTenantAlias = builder.awsTenantAlias;
    this.awsEnvironmentAlias = builder.awsEnvironmentAlias;

    this.name = builder.name;
    this.description = builder.description;
    this.properties = builder.properties;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.createdDate = builder.createdDate;
    this.lastUpdatedDate = builder.lastUpdatedDate;
  }

  /** Default constructor for subclass Builder constructor */
  protected UFWorkspaceLight() {
    this.id = null;
    this.uuid = null;
    this.cloudPlatform = null;
    this.googleProjectId = null;
    this.awsMajorVersion = null;
    this.awsOrganizationId = null;
    this.awsAccountId = null;
    this.awsTenantAlias = null;
    this.awsEnvironmentAlias = null;

    this.name = null;
    this.description = null;
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
    OUT.println("Cloud Platform:    " + cloudPlatform);

    if (cloudPlatform == CloudPlatform.GCP) {
      OUT.println("Google project:    " + googleProjectId);
      OUT.println(
          "Cloud console:     https://console.cloud.google.com/home/dashboard?project="
              + googleProjectId);

    } else if (cloudPlatform == CloudPlatform.AWS) {
      OUT.println("AWS major version: " + awsMajorVersion);
      OUT.println("AWS organization:  " + awsOrganizationId);
      OUT.println("AWS account:       " + awsAccountId);
      OUT.println("AWS tenant:        " + awsTenantAlias);
      OUT.println("AWS environment:   " + awsEnvironmentAlias);
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
    protected String id;
    protected UUID uuid;
    protected CloudPlatform cloudPlatform;

    // GCP
    protected String googleProjectId;

    // AWS
    protected String awsMajorVersion;
    protected String awsOrganizationId;
    protected String awsAccountId;
    protected String awsTenantAlias;
    protected String awsEnvironmentAlias;

    protected String name;
    protected String description;
    protected Map<String, String> properties;
    protected String serverName;
    protected String userEmail;
    protected OffsetDateTime createdDate;
    protected OffsetDateTime lastUpdatedDate;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder uuid(UUID uuid) {
      this.uuid = uuid;
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

    public Builder awsMajorVersion(String awsMajorVersion) {
      this.awsMajorVersion = awsMajorVersion;
      return this;
    }

    public Builder awsOrganizationId(String awsOrganizationId) {
      this.awsOrganizationId = awsOrganizationId;
      return this;
    }

    public Builder awsAccountId(String awsAccountId) {
      this.awsAccountId = awsAccountId;
      return this;
    }

    public Builder awsTenantAlias(String awsTenantAlias) {
      this.awsTenantAlias = awsTenantAlias;
      return this;
    }

    public Builder awsEnvironmentAlias(String awsEnvironmentAlias) {
      this.awsEnvironmentAlias = awsEnvironmentAlias;
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

    /** Call the private constructor. */
    public UFWorkspaceLight build() {
      return new UFWorkspaceLight(this);
    }
  }
}
