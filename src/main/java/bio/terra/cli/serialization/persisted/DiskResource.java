package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Resource;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.UUID;

/**
 * External representation of a workspace resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link Resource} class for a resource's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = DiskResource.Builder.class)
public abstract class DiskResource {
  public final UUID id;
  public final String name;
  public final String description;
  public final Resource.Type resourceType;
  public final StewardshipType stewardshipType;
  public final CloningInstructionsEnum cloningInstructions;
  public final AccessScope accessScope;
  public final ManagedBy managedBy;
  public final String privateUserName;
  public final List<ControlledResourceIamRole> privateUserRoles;

  /** Serialize an instance of the internal class to the disk format. */
  public DiskResource(Resource internalObj) {
    this.id = internalObj.getId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.resourceType = internalObj.getResourceType();
    this.stewardshipType = internalObj.getStewardshipType();
    this.cloningInstructions = internalObj.getCloningInstructions();
    this.accessScope = internalObj.getAccessScope();
    this.managedBy = internalObj.getManagedBy();
    this.privateUserName = internalObj.getPrivateUserName();
    this.privateUserRoles = internalObj.getPrivateUserRoles();
  }

  protected DiskResource(DiskResource.Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.resourceType = builder.resourceType;
    this.stewardshipType = builder.stewardshipType;
    this.cloningInstructions = builder.cloningInstructions;
    this.accessScope = builder.accessScope;
    this.managedBy = builder.managedBy;
    this.privateUserName = builder.privateUserName;
    this.privateUserRoles = builder.privateUserRoles;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public abstract Resource deserializeToInternal();

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private UUID id;
    private String name;
    private String description;
    private Resource.Type resourceType;
    private StewardshipType stewardshipType;
    private CloningInstructionsEnum cloningInstructions;
    private AccessScope accessScope;
    private ManagedBy managedBy;
    private String privateUserName;
    private List<ControlledResourceIamRole> privateUserRoles;

    public Builder id(UUID id) {
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

    public Builder resourceType(Resource.Type resourceType) {
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

    public Builder managedBy(ManagedBy managedBy) {
      this.managedBy = managedBy;
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
    public abstract DiskResource build();

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
