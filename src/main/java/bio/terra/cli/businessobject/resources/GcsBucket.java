package bio.terra.cli.businessobject.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resources.PDGcsBucket;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateGcsBucket;
import bio.terra.cli.serialization.userfacing.resources.UFGcsBucket;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCS bucket workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class GcsBucket extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucket.class);

  private String bucketName;

  // prefix for GCS bucket to make a valid URL.
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  /** Deserialize an instance of the disk format to the internal object. */
  public GcsBucket(PDGcsBucket configFromDisk) {
    super(configFromDisk);
    this.bucketName = configFromDisk.bucketName;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public GcsBucket(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GCS_BUCKET;
    this.bucketName = wsmObject.getResourceAttributes().getGcpGcsBucket().getBucketName();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public GcsBucket(GcpGcsBucketResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GCS_BUCKET;
    this.bucketName = wsmObject.getAttributes().getBucketName();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGcsBucket serializeToCommand() {
    return new UFGcsBucket(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGcsBucket serializeToDisk() {
    return new PDGcsBucket(this);
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GcsBucket addReferenced(CreateUpdateGcsBucket createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.resourceFields.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpGcsBucketResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedGcsBucket(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GcsBucket(addedResource);
  }

  /**
   * Create a GCS bucket as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static GcsBucket createControlled(CreateUpdateGcsBucket createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.resourceFields.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to create the resource
    GcpGcsBucketResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledGcsBucket(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GcsBucket(createdResource);
  }

  /** Delete a GCS bucket referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedGcsBucket(Context.requireWorkspace().getId(), id);
  }

  /** Delete a GCS bucket controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledGcsBucket(Context.requireWorkspace().getId(), id);
  }

  /** Resolve a GCS bucket resource to its cloud identifier. */
  public String resolve() {
    return resolve(true);
  }

  /**
   * Resolve a GCS bucket resource to its cloud identifier. Optionally include the 'gs://' prefix.
   */
  public String resolve(boolean includePrefix) {
    return includePrefix ? GCS_BUCKET_URL_PREFIX + bucketName : bucketName;
  }

  /**
   * Check whether a user can access the GCS bucket resource.
   *
   * @param credentialsToUse enum value indicates whether to use end-user or pet SA credentials for
   *     checking access
   * @return true if the user can access the referenced GCS bucket with the given credentials
   * @throws UserActionableException if the resource is CONTROLLED
   */
  public boolean checkAccess(CheckAccessCredentials credentialsToUse) {
    if (!stewardshipType.equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO (PF-717): replace this with a call to WSM once an endpoint is available
    User currentUser = Context.requireUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(Resource.CheckAccessCredentials.USER)
            ? currentUser.getUserCredentials()
            : currentUser.getPetSACredentials();

    return new GoogleCloudStorage(credentials, Context.requireWorkspace().getGoogleProjectId())
        .checkObjectsListAccess(resolve());
  }

  // ====================================================
  // Property getters.

  public String getBucketName() {
    return bucketName;
  }
}
