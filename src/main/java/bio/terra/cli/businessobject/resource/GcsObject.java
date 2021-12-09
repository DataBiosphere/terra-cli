package bio.terra.cli.businessobject.resource;

import static bio.terra.cli.businessobject.resource.GcsBucket.GCS_BUCKET_URL_PREFIX;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDGcsObject;
import bio.terra.cli.serialization.userfacing.input.CreateGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpGcsObjectResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCS bucket object workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class GcsObject extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsObject.class);

  private String bucketName;
  private String objectName;

  /** Deserialize an instance of the disk format to the internal object. */
  public GcsObject(PDGcsObject configFromDisk) {
    super(configFromDisk);
    this.bucketName = configFromDisk.bucketName;
    this.objectName = configFromDisk.objectName;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public GcsObject(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GCS_OBJECT;
    this.bucketName = wsmObject.getResourceAttributes().getGcpGcsObject().getBucketName();
    this.objectName = wsmObject.getResourceAttributes().getGcpGcsObject().getFileName();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public GcsObject(GcpGcsObjectResource resource) {
    super(resource.getMetadata());
    this.resourceType = Type.GCS_OBJECT;
    this.bucketName = resource.getAttributes().getBucketName();
    this.objectName = resource.getAttributes().getFileName();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGcsObject serializeToCommand() {
    return new UFGcsObject(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGcsObject serializeToDisk() {
    return new PDGcsObject(this);
  }

  /**
   * Add a GCS bucket object as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GcsObject addReferenced(CreateGcsObjectParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to add the reference. use the pet SA credentials instead of the end user's
    // credentials, because they include the cloud-platform scope. WSM needs the cloud-platform
    // scope to perform its access check before adding the reference. note that this means a user
    // cannot add a reference unless their pet SA has access to it.
    GcpGcsObjectResource addedResource =
        WorkspaceManagerService.fromContextForPetSa()
            .createReferencedGcsObject(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket object: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GcsObject(addedResource);
  }

  /** Update a GCS bucket object referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedGcsObjectParams updateParams) {
    if (updateParams.resourceFields != null && updateParams.resourceFields.name != null) {
      validateEnvironmentVariableName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedGcsObject(Context.requireWorkspace().getId(), id, updateParams);
    if (updateParams.bucketName != null) {
      this.bucketName = updateParams.bucketName;
    }
    if (updateParams.objectName != null) {
      this.objectName = updateParams.objectName;
    }
    super.updatePropertiesAndSync(updateParams.resourceFields);
  }

  /** Delete a GCS bucket object referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferenceGcsObject(Context.requireWorkspace().getId(), id);
  }

  protected void deleteControlled() {
    // call WSM to delete the resource
    throw new UnsupportedOperationException(
        "Does not support creating a bucket object controlled resource in workspace manager yet");
  }

  /** Resolve a GCS bucket object resource to its cloud identifier. */
  public String resolve() {
    return resolve(/*includePrefix=*/ true);
  }

  /**
   * Resolve a GCS bucket object resource to its cloud identifier. Optionally include the 'gs://'
   * prefix.
   */
  public String resolve(boolean includePrefix) {
    return includePrefix
        ? GCS_BUCKET_URL_PREFIX + bucketName + "/" + objectName
        : bucketName + "/" + objectName;
  }

  // ====================================================
  // Property getters.

  public String getBucketName() {
    return bucketName;
  }

  public String getObjectName() {
    return objectName;
  }
}
