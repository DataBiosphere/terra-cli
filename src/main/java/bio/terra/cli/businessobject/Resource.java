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
import bio.terra.workspace.model.Properties;
import bio.terra.workspace.model.Property;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.StewardshipType;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Internal representation of a workspace resource. This abstract class contains properties common
 * to all resource types. Sub-classes represent a specific resource type. Instances of the
 * sub-classes are part of the current context or state.
 */
public abstract class Resource {
  // Copied from WSM: ResourceType specific validation performed in WSM
  private static final Pattern RESOURCE_NAME_VALIDATION_PATTERN =
      Pattern.compile("^[a-zA-Z0-9][-_a-zA-Z0-9]{0,1023}$");

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
  protected ControlledResourceIamRole privateUserRole;

  protected Properties properties;

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
    this.privateUserRole = configFromDisk.privateUserRole;
    this.properties = configFromDisk.properties;
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
        this.privateUserRole = privateMetadata.getPrivateResourceIamRole();
      }
    }

    this.properties = metadata.getProperties();
  }

  /**
   * Deserialize to the internal representation of the resource from the WSM client library format.
   * Calls the appropriate sub-class constructor based on the resource type.
   */
  public static Resource deserializeFromWsm(ResourceDescription wsmObject) {
    bio.terra.workspace.model.ResourceType wsmResourceType =
        wsmObject.getMetadata().getResourceType();
    return switch (wsmResourceType) {
      case GCS_BUCKET -> new GcsBucket(wsmObject);
      case GCS_OBJECT -> new GcsObject(wsmObject);
      case BIG_QUERY_DATASET -> new BqDataset(wsmObject);
      case BIG_QUERY_DATA_TABLE -> new BqTable(wsmObject);
      case AI_NOTEBOOK -> new GcpNotebook(wsmObject);
      case GIT_REPO -> new GitRepo(wsmObject);
        // Omit other resource types are not supported by the CLI.
      default -> null;
    };
  }

  protected static void validateResourceName(String name) {
    if (StringUtils.isEmpty(name) || !RESOURCE_NAME_VALIDATION_PATTERN.matcher(name).matches())
      throw new UserActionableException(
          "Invalid resource name specified. Name must be 1 to 1024 alphanumeric characters, underscores, and dashes and must not start with a dash or underscore.");
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public abstract UFResource serializeToCommand();

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public abstract PDResource serializeToDisk();

  /** Update the properties of this resource object that are common to all resource types. */
  protected void updatePropertiesAndSync(UpdateResourceParams updateParams) {
    this.name = updateParams.name == null ? name : updateParams.name;
    this.description = updateParams.description == null ? description : updateParams.description;
    Context.requireWorkspace().listResources();
  }

  /** Delete an existing resource in the workspace. */
  public void delete() {
    switch (stewardshipType) {
      case REFERENCED -> deleteReferenced();
      case CONTROLLED -> deleteControlled();
      default -> throw new IllegalArgumentException("Unknown stewardship type: " + stewardshipType);
    }
    Context.requireWorkspace().listResources();
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
        .checkAccess(Context.requireWorkspace().getUuid(), id);
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

  public UUID getId() {
    return id;
  }

  // ====================================================
  // Property getters.

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

  public ControlledResourceIamRole getPrivateUserRole() {
    return privateUserRole;
  }

  public Properties getProperties() {
    return properties;
  }

  public String getProperty(String key) {
    return properties.stream()
        .filter(p -> p.getKey().equals(key))
        .findFirst()
        .map(Property::getValue)
        .orElse(null);
  }

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
    GIT_REPO
  }
}
