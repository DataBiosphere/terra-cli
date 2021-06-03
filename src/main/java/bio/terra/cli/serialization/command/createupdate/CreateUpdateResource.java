package bio.terra.cli.serialization.command.createupdate;

import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

/**
 * Parameters for creating/updating a workspace resource. This class is not currently user-facing,
 * but could be exposed as a command input format in the future. This class handles properties that
 * are common to all resource types. Sub-classes include additional resource-type specific
 * properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateUpdateResource.Builder.class)
public class CreateUpdateResource {
  public final String name;
  public final String description;
  public final ResourceType resourceType;
  public final StewardshipType stewardshipType;
  public final CloningInstructionsEnum cloningInstructions;
  public final AccessScope accessScope;
  public final String privateUserName;
  public final List<ControlledResourceIamRole> privateUserRoles;

  protected CreateUpdateResource(Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.resourceType = builder.resourceType;
    this.stewardshipType = builder.stewardshipType;
    this.cloningInstructions = builder.cloningInstructions;
    this.accessScope = builder.accessScope;
    this.privateUserName = builder.privateUserName;
    this.privateUserRoles = builder.privateUserRoles;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String description;
    private ResourceType resourceType;
    private StewardshipType stewardshipType;
    private CloningInstructionsEnum cloningInstructions;
    private AccessScope accessScope;
    private String privateUserName;
    private List<ControlledResourceIamRole> privateUserRoles;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder resourceType(ResourceType resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder stewardshipType(StewardshipType stewardshipType) {
      this.stewardshipType = stewardshipType;
      return this;
    }

    public Builder cloningInstructions(CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    public Builder accessScope(AccessScope accessScope) {
      this.accessScope = accessScope;
      return this;
    }

    public Builder privateUserName(String privateUserName) {
      this.privateUserName = privateUserName;
      return this;
    }

    public Builder privateUserRoles(List<ControlledResourceIamRole> privateUserRoles) {
      this.privateUserRoles = privateUserRoles;
      return this;
    }

    /** Call the private constructor. */
    public CreateUpdateResource build() {
      return new CreateUpdateResource(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
