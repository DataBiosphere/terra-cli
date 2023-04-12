package bio.terra.cli.businessobject.resource;

import static bio.terra.cli.businessobject.resource.GcsBucket.GCS_BUCKET_URL_PREFIX;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.resource.PDGcsObject;
import bio.terra.cli.serialization.userfacing.input.AddGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsObject;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BlobCow;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.workspace.model.GcpGcsObjectResource;
import bio.terra.workspace.model.ResourceDescription;
import com.google.cloud.storage.Storage.BlobListOption;
import java.util.stream.StreamSupport;
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
   * Add a GCS bucket object as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GcsObject addReferenced(AddGcsObjectParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    GcpGcsObjectResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedGcsObject(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created GCS bucket object: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResources();
    return new GcsObject(addedResource);
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

  /** Update a GCS bucket object referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedGcsObjectParams updateParams) {
    UpdateResourceParams resourceParams = updateParams.resourceFields;
    if (resourceParams.name != null) {
      validateResourceName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedGcsObject(Context.requireWorkspace().getUuid(), id, updateParams);
    if (updateParams.bucketName != null) {
      this.bucketName = updateParams.bucketName;
    }
    if (updateParams.objectName != null) {
      this.objectName = updateParams.objectName;
    }
    if (updateParams.cloningInstructions != null) {
      this.cloningInstructions = updateParams.cloningInstructions;
    }
    super.updatePropertiesAndSync(resourceParams);
  }

  /** Delete a GCS bucket object referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedGcsObject(Context.requireWorkspace().getUuid(), id);
  }

  protected void deleteControlled() {
    // call WSM to delete the resource
    throw new UnsupportedOperationException(
        "Does not support creating a bucket object controlled resource in workspace manager yet");
  }

  /** Resolve a GCS bucket object resource to its cloud identifier. */
  public String resolve() {
    return resolve(/*includeUrlPrefix=*/ true);
  }

  /**
   * Resolve a GCS bucket object resource to its cloud identifier. Optionally include the 'gs://'
   * prefix.
   */
  public String resolve(boolean includeUrlPrefix) {
    return includeUrlPrefix
        ? GCS_BUCKET_URL_PREFIX + bucketName + "/" + objectName
        : bucketName + "/" + objectName;
  }

  /**
   * Check if the GCS bucket object is a directory by checking for the presence of a child object
   * with a trailing slash. There will always be an object present to represent the directory/prefix
   * itself.
   */
  public boolean isDirectory() throws SystemException {
    try {
      BucketCow bucketCow =
          CrlUtils.createStorageCow(Context.requireUser().getPetSACredentials()).get(bucketName);
      Iterable<BlobCow> objects =
          bucketCow
              .list(BlobListOption.currentDirectory(), BlobListOption.prefix(objectName))
              .getValues();
      return StreamSupport.stream(objects.spliterator(), false)
          .anyMatch(object -> object.getBlobInfo().isDirectory());
    } catch (Exception e) {
      throw new SystemException("Error looking up bucket: " + bucketName, e);
    }
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
