package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resource.UFGcpNotebook;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

/**
 * External representation of a workspace resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Resource} class for a resource's internal representation.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "resourceType")
@JsonSubTypes({
  @Type(value = UFGcpNotebook.class, name = "AI_NOTEBOOK"),
  @Type(value = UFBqDataset.class, name = "BQ_DATASET"),
  @Type(value = UFGcsBucket.class, name = "GCS_BUCKET"),
  @Type(value = UFBqDataTable.class, name = "BQ_DATA_TABLE")
})
@JsonDeserialize(builder = UFResource.Builder.class)
public abstract class UFResource {
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

  /** Serialize an instance of the internal class to the command format. */
  public UFResource(Resource internalObj) {
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

  /** Constructor for Jackson deserialization during testing. */
  protected UFResource(Builder builder) {
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

  /**
   * Print out this object in text format.
   *
   * @param prefix string to prepend to all printed lines
   */
  public void print(String prefix) {
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Name:         " + (name == null ? "" : name));
    OUT.println(prefix + "Description:  " + (description == null ? "" : description));
    OUT.println(prefix + "Type:         " + resourceType);
    OUT.println(prefix + "Stewardship:  " + stewardshipType);
    OUT.println(prefix + "Cloning:      " + cloningInstructions);

    if (stewardshipType.equals(StewardshipType.CONTROLLED)) {
      OUT.println(prefix + "Access scope: " + accessScope);
      OUT.println(prefix + "Managed by:   " + managedBy);

      if (accessScope.equals(AccessScope.PRIVATE_ACCESS)) {
        OUT.println(prefix + "Private user: " + privateUserName);
      }
    }
  }

  /** Print out this object in text format. */
  public void print() {
    print("");
  }

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
    public abstract UFResource build();

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
