package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDGcsBucketFile;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketFileParams;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucketFile;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpGcsBucketFileResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCS bucket file workspace resource. Instances of this class are part
 * of the current context or state.
 */
public class GcsBucketFile extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucketFile.class);

  private String bucketName;
  private String filePath;

  // prefix for GCS bucket to make a valid URL.
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  /** Deserialize an instance of the disk format to the internal object. */
  public GcsBucketFile(PDGcsBucketFile configFromDisk) {
    super(configFromDisk);
    this.bucketName = configFromDisk.bucketName;
    this.filePath = configFromDisk.filePath;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public GcsBucketFile(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GCS_BUCKET_FILE;
    this.bucketName = wsmObject.getResourceAttributes().getGcpGcsBucketFile().getBucketName();
    this.filePath = wsmObject.getResourceAttributes().getGcpGcsBucketFile().getFileName();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public GcsBucketFile(GcpGcsBucketFileResource resource) {
    super(resource.getMetadata());
    this.resourceType = Type.GCS_BUCKET_FILE;
    this.bucketName = resource.getAttributes().getBucketName();
    this.filePath = resource.getAttributes().getFileName();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGcsBucketFile serializeToCommand() {
    return new UFGcsBucketFile(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGcsBucketFile serializeToDisk() {
    return new PDGcsBucketFile(this);
  }

  /**
   * Add a GCS bucket file as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GcsBucketFile addReferenced(CreateGcsBucketFileParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to add the reference. use the pet SA credentials instead of the end user's
    // credentials, because they include the cloud-platform scope. WSM needs the cloud-platform
    // scope to perform its access check before adding the reference. note that this means a user
    // cannot add a reference unless their pet SA has access to it.
    GcpGcsBucketFileResource addedResource =
        WorkspaceManagerService.fromContextForPetSa()
            .createReferencedGcsBucketFile(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket file: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GcsBucketFile(addedResource);
  }

  /** Update a GCS bucket file referenced resource in the workspace. */
  public void updateReferenced(UpdateResourceParams updateParams) {
    if (updateParams.name != null) {
      validateEnvironmentVariableName(updateParams.name);
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedGcsBucketFile(Context.requireWorkspace().getId(), id, updateParams);
    super.updatePropertiesAndSync(updateParams);
  }

  /** Delete a GCS bucket file referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedGcsBucketFile(Context.requireWorkspace().getId(), id);
  }

  protected void deleteControlled() {
    // call WSM to delete the resource
    throw new UnsupportedOperationException(
        "Does not support creating a bucket file controlled resource in workspace manager yet");
  }

  /** Resolve a GCS bucket file resource to its cloud identifier. */
  public String resolve() {
    return resolve(/*includePrefix=*/ true);
  }

  /**
   * Resolve a GCS bucket file resource to its cloud identifier. Optionally include the 'gs://'
   * prefix.
   */
  public String resolve(boolean includePrefix) {
    return includePrefix
        ? GCS_BUCKET_URL_PREFIX + bucketName + "/" + filePath
        : bucketName + "/" + filePath;
  }

  // ====================================================
  // Property getters.

  public String getBucketName() {
    return bucketName;
  }

  public String getFilePath() {
    return filePath;
  }
}
