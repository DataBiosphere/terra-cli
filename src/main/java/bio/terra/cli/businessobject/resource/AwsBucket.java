package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDAwsBucket;
import bio.terra.cli.serialization.userfacing.input.CreateAwsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledAwsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedAwsBucketParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsBucket;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import bio.terra.workspace.model.AwsBucketResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS bucket workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class AwsBucket extends Resource {
  // prefix for AWS bucket to make a valid URL.
  private static final String AWS_BUCKET_URL_PREFIX = "s3://";
  private static final Logger logger = LoggerFactory.getLogger(AwsBucket.class);
  private final String bucketName;
  private final String bucketPrefix;
  private final String location;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsBucket(PDAwsBucket configFromDisk) {
    super(configFromDisk);
    this.bucketName = configFromDisk.bucketName;
    this.bucketPrefix = configFromDisk.bucketPrefix;
    this.location = configFromDisk.location;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsBucket(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_BUCKET;
    this.bucketName = wsmObject.getResourceAttributes().getAwsBucket().getS3BucketName();
    this.bucketPrefix = wsmObject.getResourceAttributes().getAwsBucket().getPrefix();
    this.location = wsmObject.getResourceAttributes().getAwsBucket().getRegion();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsBucket(AwsBucketResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_BUCKET;
    this.bucketName = wsmObject.getAttributes().getS3BucketName();
    this.bucketPrefix = wsmObject.getAttributes().getPrefix();
    this.location = wsmObject.getAttributes().getRegion();
  }

  /**
   * Add a AWS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static AwsBucket addReferenced(CreateAwsBucketParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    AwsBucketResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedAwsBucket(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS bucket: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new AwsBucket(addedResource);
  }

  /**
   * Create a AWS bucket as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsBucket createControlled(CreateAwsBucketParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AwsBucketResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledAwsBucket(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new AwsBucket(createdResource);
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsBucket serializeToCommand() {
    return new UFAwsBucket(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsBucket serializeToDisk() {
    return new PDAwsBucket(this);
  }

  /** Update a AWS bucket referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedAwsBucketParams updateParams) {
    if (updateParams.resourceParams.name != null) {
      validateResourceName(updateParams.resourceParams.name);
    }
    if (updateParams.cloningInstructions != null) {
      this.cloningInstructions = updateParams.cloningInstructions;
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedAwsBucket(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceParams);
  }

  /** Update a AWS bucket controlled resource in the workspace. */
  public void updateControlled(UpdateControlledAwsBucketParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateResourceName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateControlledAwsBucket(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
  }

  /** Delete a AWS bucket referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedAwsBucket(Context.requireWorkspace().getUuid(), id);
  }

  /** Delete a AWS bucket controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledAwsBucket(Context.requireWorkspace().getUuid(), id);
  }

  /** Resolve a AWS bucket resource to its cloud identifier. */
  public String resolve() {
    return resolve(true);
  }

  /**
   * Resolve a AWS bucket resource to its cloud identifier. Optionally include the 's3://' prefix.
   */
  public String resolve(boolean includeUrlPrefix) {
    return resolve(bucketName, bucketPrefix, includeUrlPrefix);
  }

  /**
   * Resolve a AWS bucket resource to its cloud identifier. Optionally include the 's3://' prefix.
   */
  public static String resolve(
      String awsBucketName, String awsBucketPrefix, boolean includeUrlPrefix) {
    String resolvedPath = String.format("%s/%s/", awsBucketName, awsBucketPrefix);
    return includeUrlPrefix ? AWS_BUCKET_URL_PREFIX + resolvedPath : resolvedPath;
  }

  /** Query the cloud for information about the bucket. */
  public Optional<byte[]> getBucket() {
    // TODO(TERRA-206) change to AWS BucketCow
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

  public String getBucketPrefix() {
    return bucketPrefix;
  }

  public String getLocation() {
    return location;
  }
}
