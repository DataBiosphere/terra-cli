package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDGcsBucket;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.input.controlled.CreateGcsBucketParams;
<<<<<<< HEAD
import bio.terra.cli.serialization.userfacing.input.controlled.UpdateControlledGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
=======
import bio.terra.cli.serialization.userfacing.input.controlled.UpdateGcsBucketParams;
>>>>>>> a77ab26 (add test)
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
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
  protected static final String GCS_BUCKET_URL_PREFIX = "gs://";

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
  public static GcsBucket addReferenced(CreateGcsBucketParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to add the reference. use the pet SA credentials instead of the end user's
    // credentials, because they include the cloud-platform scope. WSM needs the cloud-platform
    // scope to perform its access check before adding the reference. note that this means a user
    // cannot add a reference unless their pet SA has access to it.
    GcpGcsBucketResource addedResource =
        WorkspaceManagerService.fromContextForPetSa()
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
  public static GcsBucket createControlled(CreateGcsBucketParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to create the resource
    GcpGcsBucketResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledGcsBucket(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GcsBucket(createdResource);
  }

  /** Update a GCS bucket referenced resource in the workspace. */
  public void updateReferenced(UpdateResourceParams updateParams) {
    if (updateParams.name != null) {
      validateEnvironmentVariableName(updateParams.name);
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedGcsBucket(Context.requireWorkspace().getId(), id, updateParams);
    super.updatePropertiesAndSync(updateParams);
  }

  /** Update a GCS bucket controlled resource in the workspace. */
  public void updateControlled(UpdateControlledGcsBucketParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateEnvironmentVariableName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateControlledGcsBucket(Context.requireWorkspace().getId(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
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

  /** Query the cloud for information about the bucket. */
  public Optional<BucketCow> getBucket() {
    try {
      StorageCow storageCow =
          CrlUtils.createStorageCow(Context.requireUser().getPetSACredentials());
      return Optional.of(storageCow.get(bucketName));
    } catch (Exception ex) {
      logger.error("Caught exception looking up bucket", ex);
      return Optional.empty();
    }
  }

  // ====================================================
  // Property getters.

  public String getBucketName() {
    return bucketName;
  }
}
