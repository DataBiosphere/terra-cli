package bio.terra.cli.businessobject;

import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.businessobject.resource.GcpNotebook;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.businessobject.resource.GitRepo;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.PDResource;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.StewardshipType;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Internal representation of a workspace resource. This abstract class contains properties common
 * to all resource types. Sub-classes represent a specific resource type. Instances of the
 * sub-classes are part of the current context or state.
 */
public abstract class Resource {
  // all resources
  protected UUID id;
  protected String name;
  protected String description;
  protected Type resourceType;
  protected StewardshipType stewardshipType;
  protected CloningInstructionsEnum cloningInstructions;

  // controlled resources
  protected AccessScope accessScope;
  protected ManagedBy managedBy;

  // private controlled resources
  protected String privateUserName;
  protected List<ControlledResourceIamRole> privateUserRoles;

  /**
   * Enum for the types of workspace resources supported by the CLI. Each enum value maps to a
   * single WSM client library ({@link bio.terra.workspace.model.ResourceType}) enum value.
   *
   * <p>The CLI defines its own enum instead of using the WSM one so that we can restrict the
   * resource types supported (e.g. no Data Repo snapshots). It also gives the CLI control over what
   * the enum names are, which are exposed to users as command options.
   */
  public enum Type {
    GCS_BUCKET,
    GCS_OBJECT,
    BQ_DATASET,
    BQ_TABLE,
    AI_NOTEBOOK,
    GIT_REPO;
  }

  /** Deserialize an instance of the disk format to the internal object. */
  protected Resource(PDResource configFromDisk) {
    this.id = configFromDisk.id;
    this.name = configFromDisk.name;
    this.description = configFromDisk.description;
    this.resourceType = configFromDisk.resourceType;
    this.stewardshipType = configFromDisk.stewardshipType;
    this.cloningInstructions = configFromDisk.cloningInstructions;
    this.accessScope = configFromDisk.accessScope;
    this.managedBy = configFromDisk.managedBy;
    this.privateUserName = configFromDisk.privateUserName;
    this.privateUserRoles = configFromDisk.privateUserRoles;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  protected Resource(ResourceMetadata metadata) {
    this.id = metadata.getResourceId();
    this.name = metadata.getName();
    this.description = metadata.getDescription();
    this.stewardshipType = metadata.getStewardshipType();
    this.cloningInstructions = metadata.getCloningInstructions();

    if (stewardshipType.equals(StewardshipType.CONTROLLED)) {
      ControlledResourceMetadata controlledMetadata = metadata.getControlledResourceMetadata();
      this.accessScope = controlledMetadata.getAccessScope();
      this.managedBy = controlledMetadata.getManagedBy();

      PrivateResourceUser privateMetadata = controlledMetadata.getPrivateResourceUser();
      if (accessScope.equals(AccessScope.PRIVATE_ACCESS)) {
        this.privateUserName = privateMetadata.getUserName();
        this.privateUserRoles = privateMetadata.getPrivateResourceIamRoles();
      }
    }
  }

  /**
   * Deserialize to the internal representation of the resource from the WSM client library format.
   * Calls the appropriate sub-class constructor based on the resource type.
   */
  public static Resource deserializeFromWsm(ResourceDescription wsmObject) {
    bio.terra.workspace.model.ResourceType wsmResourceType =
        wsmObject.getMetadata().getResourceType();
    switch (wsmResourceType) {
      case GCS_BUCKET:
        return new GcsBucket(wsmObject);
      case GCS_OBJECT:
        return new GcsObject(wsmObject);
      case BIG_QUERY_DATASET:
        return new BqDataset(wsmObject);
      case BIG_QUERY_DATA_TABLE:
        return new BqTable(wsmObject);
      case AI_NOTEBOOK:
        return new GcpNotebook(wsmObject);
      case GIT_REPO:
        return new GitRepo(wsmObject);
      default:
        throw new IllegalArgumentException("Unexpected resource type: " + wsmResourceType);
    }
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public abstract UFResource serializeToCommand();

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public abstract PDResource serializeToDisk();

  /**
   * Check if the name only contains alphanumeric and underscore characters.
   *
   * <p>When launching an app, either in a Docker container or a local process, we pass a map of all
   * the resources in the workspace to their resolved cloud identifiers (e.g. TERRA_MYBUCKET ->
   * gs://mybucket). This is the reason for this restriction at resource creation time.
   *
   * @param name string to check
   * @throws UserActionableException if the string is not a valid environment variable name
   */
  protected static void validateEnvironmentVariableName(String name) {
    if (Pattern.compile("[^a-zA-Z0-9_]").matcher(name).find()) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }
  }

  /** Update the properties of this resource object that are common to all resource types. */
  protected void updatePropertiesAndSync(UpdateResourceParams updateParams) {
    this.name = updateParams.name == null ? name : updateParams.name;
    this.description = updateParams.description == null ? description : updateParams.description;
    Context.requireWorkspace().listResourcesAndSync();
  }

  /** Delete an existing resource in the workspace. */
  public void delete() {
    switch (stewardshipType) {
      case REFERENCED:
        deleteReferenced();
        break;
      case CONTROLLED:
        deleteControlled();
        break;
      default:
        throw new IllegalArgumentException("Unknown stewardship type: " + stewardshipType);
    }
    Context.requireWorkspace().listResourcesAndSync();
  }

  /** Call WSM to delete a referenced resource. */
  protected abstract void deleteReferenced();

  /** Call WSM to delete a controlled resource. */
  protected abstract void deleteControlled();

  /** Resolve a resource to its cloud identifier. */
  public abstract String resolve();

  /**
   * Check whether a user's pet SA can access a resource.
   *
   * @return true if the user can access the referenced resource with the given credentials
   * @throws UserActionableException if the resource is CONTROLLED
   */
  public boolean checkAccess() {
    if (!stewardshipType.equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }
    // call WSM to check access to the resource
    return WorkspaceManagerService.fromContext()
        .checkAccess(Context.requireWorkspace().getId(), id);
  }

  /**
   * Cast this resource to a specific type (i.e. a sub-class of this class).
   *
   * @throws UserActionableException if the resource is the wrong type
   */
  public <T extends Resource> T castToType(Resource.Type type) {
    if (!resourceType.equals(type)) {
      throw new UserActionableException("Invalid resource type: " + resourceType);
    }
    return (T) this;
  }

  // ====================================================
  // Property getters.

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Type getResourceType() {
    return resourceType;
  }

  public StewardshipType getStewardshipType() {
    return stewardshipType;
  }

  public CloningInstructionsEnum getCloningInstructions() {
    return cloningInstructions;
  }

  public AccessScope getAccessScope() {
    return accessScope;
  }

  public ManagedBy getManagedBy() {
    return managedBy;
  }

  public String getPrivateUserName() {
    return privateUserName;
  }

  public List<ControlledResourceIamRole> getPrivateUserRoles() {
    return privateUserRoles;
  }
}
