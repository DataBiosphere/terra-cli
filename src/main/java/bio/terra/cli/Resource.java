package bio.terra.cli;

import bio.terra.cli.resources.AiNotebook;
import bio.terra.cli.resources.BqDataset;
import bio.terra.cli.resources.GcsBucket;
import bio.terra.cli.resources.ResourceType;
import bio.terra.cli.serialization.disk.DiskResource;
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
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Resource {
  // all resources
  protected UUID id;
  protected String name;
  protected String description;
  protected ResourceType resourceType;
  protected StewardshipType stewardshipType;
  protected CloningInstructionsEnum cloningInstructions;

  // controlled resources
  protected AccessScope accessScope;
  protected ManagedBy managedBy;

  // private controlled resources
  protected String privateUserName;
  protected List<ControlledResourceIamRole> privateUserRoles;

  protected Resource(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.resourceType = builder.getResourceType();
    this.stewardshipType = builder.stewardshipType;
    this.cloningInstructions = builder.cloningInstructions;

    this.accessScope = builder.accessScope;
    this.managedBy = builder.managedBy;

    this.privateUserName = builder.privateUserName;
    this.privateUserRoles = builder.privateUserRoles;
  }

  /**
   * Check if the name only contains alphanumeric and underscore characters. Sync the cached list of
   * resources.
   *
   * <p>When launching an app, either in a Docker container or a local process, we pass a map of all
   * the resources in the workspace to their resolved cloud identifiers (e.g. TERRA_MYBUCKET ->
   * gs://mybucket). This is the reason for this restriction at resource creation time.
   *
   * @param name string to check
   * @return true if the string is a valid environment variable name
   */
  protected static boolean isValidEnvironmentVariableName(String name) {
    return !Pattern.compile("[^a-zA-Z0-9_]").matcher(name).find();
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
    listAndSync();
  }

  /** Call WSM to delete a referenced resource. */
  protected abstract void deleteReferenced();

  /** Call WSM to delete a controlled resource. */
  protected abstract void deleteControlled();

  /** Resolve a resource to its cloud identifier. */
  public abstract String resolve();

  /**
   * Helper enum for the {@link #checkAccess(CheckAccessCredentials)} method. Specifies whether to
   * use end-user or pet SA credentials for checking access to a resource in the workspace.
   */
  public enum CheckAccessCredentials {
    USER,
    PET_SA;
  };

  /** Call WSM to check whether the current user has access to a resource. */
  public abstract boolean checkAccess(CheckAccessCredentials credentialsToUse);

  /** Fetch the list of resources for the current workspace. Sync the cached list of resources. */
  public static List<Resource> listAndSync() {
    List<ResourceDescription> wsmObjects =
        new WorkspaceManagerService()
            .enumerateAllResources(
                Context.requireWorkspace().getId(), Context.getConfig().getResourcesCacheSize());
    List<Resource> resources =
        wsmObjects.stream()
            .map(wsmObject -> Builder.fromWSMObject(wsmObject).build())
            .collect(Collectors.toList());

    Context.requireWorkspace().setResources(resources);
    return resources;
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

  public ResourceType getResourceType() {
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

  /**
   * Builder class to help construct an immutable Resource object with lots of properties.
   * Sub-classes extend this with resource type-specific properties.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private UUID id;
    private String name;
    private String description;
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

    /** Method that returns the resource type. Should be hard-coded in sub-classes. */
    public abstract ResourceType getResourceType();

    /** Call the sub-class constructor. */
    public abstract Resource build();

    /**
     * Populate this Builder object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to all resource types.
     */
    protected Builder(ResourceMetadata wsmObject) {
      this.id = wsmObject.getResourceId();
      this.name = wsmObject.getName();
      this.description = wsmObject.getDescription();
      this.stewardshipType = wsmObject.getStewardshipType();
      this.cloningInstructions = wsmObject.getCloningInstructions();

      if (stewardshipType.equals(StewardshipType.CONTROLLED)) {
        ControlledResourceMetadata controlledMetadata = wsmObject.getControlledResourceMetadata();
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
     * Populate this Builder object with properties from the on-disk object. This method handles the
     * fields that apply to all resource types.
     */
    protected Builder(DiskResource configFromDisk) {
      this.id = configFromDisk.id;
      this.name = configFromDisk.name;
      this.description = configFromDisk.description;
      this.stewardshipType = configFromDisk.stewardshipType;
      this.cloningInstructions = configFromDisk.cloningInstructions;
      this.accessScope = configFromDisk.accessScope;
      this.managedBy = configFromDisk.managedBy;
      this.privateUserName = configFromDisk.privateUserName;
      this.privateUserRoles = configFromDisk.privateUserRoles;
    }

    /** Helper method to get the appropriate sub-class of Builder for the given resource type. */
    public static Builder fromWSMObject(ResourceDescription wsmObject) {
      switch (wsmObject.getMetadata().getResourceType()) {
        case GCS_BUCKET:
          return new GcsBucket.Builder(wsmObject);
        case BIG_QUERY_DATASET:
          return new BqDataset.Builder(wsmObject);
        case AI_NOTEBOOK:
          return new AiNotebook.Builder(wsmObject);
        default:
          throw new IllegalArgumentException(
              "Unexpected resource type: " + wsmObject.getMetadata().getResourceType());
      }
    }
  }
}
