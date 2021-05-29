package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.resources.GcsBucket;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ControlledResourceIamRole;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POJO class that represents a workspace resource. This class is serialized to disk as part of the
 * global context. It is also used as the user-facing JSON output for commands that return a
 * resource.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Resource {
  private static final Logger logger = LoggerFactory.getLogger(Resource.class);

  // all resources
  public final UUID resourceId;
  public final String name;
  public final String description;
  public final ResourceType resourceType;
  public final StewardshipType stewardshipType;
  public final CloningInstructionsEnum cloningInstructions;

  // controlled resources
  public final AccessScope accessScope;
  public final ManagedBy managedBy;

  // private controlled resources
  public final String privateUserName;
  public final List<ControlledResourceIamRole> privateUserRoles;

  public Resource(ResourceBuilder builder) {
    this.resourceId = builder.resourceId;
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

  /** Add or create a new resource in the workspace. */
  public Resource addOrCreate() {
    if (!isValidEnvironmentVariableName(name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }
    Resource resource;
    switch (stewardshipType) {
      case REFERENCED:
        resource = addReferenced();
        break;
      case CONTROLLED:
        resource = createControlled();
        break;
      default:
        throw new IllegalArgumentException("Unknown stewardship type: " + stewardshipType);
    }

    // update the global context with the new resource
    GlobalContext.get().requireCurrentWorkspace().addResource(resource);
    return resource;
  }

  /** Subclass-specific method to add a referenced resource. */
  protected abstract <T extends Resource> T addReferenced();

  /** Subclass-specific method to create a controlled resource. */
  protected abstract <T extends Resource> T createControlled();

  /**
   * Check if the name only contains alphanumeric and underscore characters. Sync the cached list of
   * resources.
   *
   * @param name string to check
   * @return true if the string is a valid environment variable name
   */
  private static boolean isValidEnvironmentVariableName(String name) {
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

    // update the global context with the removed resource
    GlobalContext.get().requireCurrentWorkspace().removeResource(name);
  }

  /** Subclass-specific method to delete a referenced resource. */
  protected abstract void deleteReferenced();

  /** Subclass-specific method to delete a controlled resource. */
  protected abstract void deleteControlled();

  /** Subclass-specific method to resolve a resource. */
  public abstract String resolve();
  /**
   * case BIG_QUERY_DATASET: return getBigQueryDatasetPath(resource); case AI_NOTEBOOK: return
   * getAiNotebookInstanceName(resource);
   */

  /**
   * Helper enum for the {@link #checkAccess(CheckAccessCredentials)} method. Specifies whether to
   * use end-user or pet SA credentials for checking access to a resource in the workspace.
   */
  public enum CheckAccessCredentials {
    USER,
    PET_SA;
  };

  /** Subclass-specific method to check whether the current user has access to a resource. */
  public abstract boolean checkAccess(CheckAccessCredentials credentialsToUse);

  /** Fetch the list of resources for the current workspace. Sync the cached list of resources. */
  public static List<Resource> listAndSync() {
    GlobalContext globalContext = GlobalContext.get();
    List<ResourceDescription> wsmObjects =
        new WorkspaceManagerService()
            .enumerateAllResources(
                globalContext.requireCurrentWorkspace().id, globalContext.resourcesCacheSize);
    List<Resource> resources =
        wsmObjects.stream()
            .map(wsmObject -> ResourceBuilder.fromWSMObject(wsmObject).build())
            .collect(Collectors.toList());

    globalContext.requireCurrentWorkspace().setResources(resources);
    return resources;
  }

  /** Print out a resource object in text format. */
  public void printText() {
    PrintStream OUT = Printer.getOut();
    OUT.println("Name:         " + name);
    OUT.println("Description:  " + description);
    OUT.println("Stewardship:  " + stewardshipType);
    OUT.println("Cloning:      " + cloningInstructions);

    if (stewardshipType.equals(StewardshipType.CONTROLLED)) {
      OUT.println("Access scope: " + accessScope);
      OUT.println("Managed by:   " + managedBy);

      if (accessScope.equals(AccessScope.PRIVATE_ACCESS)) {
        OUT.println("Private user: " + privateUserName);
      }
    }
  }

  /**
   * Builder class to help construct an immutable Resource object with lots of properties.
   * Sub-classes extend this with resource type-specific properties.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class ResourceBuilder {
    private UUID resourceId;
    private String name;
    private String description;
    private StewardshipType stewardshipType;
    private CloningInstructionsEnum cloningInstructions;
    public AccessScope accessScope;
    public ManagedBy managedBy;
    public String privateUserName;
    public List<ControlledResourceIamRole> privateUserRoles;

    public ResourceBuilder resourceId(UUID resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public ResourceBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ResourceBuilder description(String description) {
      this.description = description;
      return this;
    }

    public ResourceBuilder stewardshipType(StewardshipType stewardshipType) {
      this.stewardshipType = stewardshipType;
      return this;
    }

    public ResourceBuilder cloningInstructions(CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    public ResourceBuilder accessScope(AccessScope accessScope) {
      this.accessScope = accessScope;
      return this;
    }

    public ResourceBuilder managedBy(ManagedBy managedBy) {
      this.managedBy = managedBy;
      return this;
    }

    public ResourceBuilder privateUserName(String privateUserName) {
      this.privateUserName = privateUserName;
      return this;
    }

    public ResourceBuilder privateUserRoles(List<ControlledResourceIamRole> privateUserRoles) {
      this.privateUserRoles = privateUserRoles;
      return this;
    }

    /** Subclass-specific method that returns the resource type. */
    public abstract ResourceType getResourceType();

    /** Subclass-specific method that calls the sub-class constructor. */
    public abstract Resource build();

    /** Default constructor for Jackson. */
    protected ResourceBuilder() {}

    /**
     * Populate this Resource object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to all resource types.
     */
    protected ResourceBuilder(ResourceMetadata wsmObject) {
      this.resourceId = wsmObject.getResourceId();
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
     * Helper method to get the appropriate sub-class of ResourceBuilder for the given resource
     * type.
     */
    public static ResourceBuilder fromWSMObject(ResourceDescription wsmObject) {
      switch (wsmObject.getMetadata().getResourceType()) {
        case GCS_BUCKET:
          return new GcsBucket.GcsBucketBuilder(wsmObject);
        default:
          throw new IllegalArgumentException(
              "Unexpected resource type: " + wsmObject.getMetadata().getResourceType());
      }
    }
  }
}
