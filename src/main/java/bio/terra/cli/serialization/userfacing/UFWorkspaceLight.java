package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.Properties;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

public class UFWorkspaceLight {
  // "id" instead of "userFacingId" because user sees this with "terra workspace describe
  // --format=json"
  public String id;
  public String name;
  public String description;
  public String googleProjectId;
  public Properties properties;
  public String serverName;
  public String userEmail;

  /**
   * It's expected that the workspace passed into this constructor does not have its resources
   * populated. If it does, then one should create a UFWorkspace instead.
   *
   * @param internalObj
   */
  public UFWorkspaceLight(Workspace internalObj) {
    this.id = internalObj.getUserFacingId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.googleProjectId = internalObj.getGoogleProjectId();
    this.properties = internalObj.getProperties();
    this.serverName = internalObj.getServerName();
    this.userEmail = internalObj.getUserEmail();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspaceLight(UFWorkspaceLight.Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.properties = builder.properties;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
  }

  /** Default constructor for subclass Builder constructor */
  protected UFWorkspaceLight() {
    this.id = null;
    this.name = null;
    this.description = null;
    this.googleProjectId = null;
    this.properties = null;
    this.serverName = null;
    this.userEmail = null;
  }

  /** Print out a workspace object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    // "id" instead of "userFacingId" because user sees this with "terra workspace describe
    // --format=json"
    OUT.println("ID:                " + id);
    OUT.println("Name:              " + name);
    OUT.println("Description:       " + description);
    OUT.println("Google project:    " + googleProjectId);
    OUT.println("Properties:");
    for (int counter = 0; counter < properties.size(); counter++) {
      OUT.println(
          "   " + properties.get(counter).getKey() + ": " + properties.get(counter).getValue());
    }
    OUT.println(
        "Cloud console:     https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    // "id" instead of "userFacingId" because user sees this with "terra workspace describe
    // --format=json"
    private String id;
    private String name;
    private String description;
    private String googleProjectId;
    private Properties properties;
    private String serverName;
    private String userEmail;

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

    public UFWorkspaceLight.Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public UFWorkspaceLight.Builder properties(Properties properties) {
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

    /** Call the private constructor. */
    public UFWorkspaceLight build() {
      return new UFWorkspaceLight(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
