package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsStorageFolder;
import bio.terra.cli.serialization.userfacing.input.CreateAwsStorageFolderParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsStorageFolder;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AwsStorageFolderResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS storage folder workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class AwsStorageFolder extends Resource {
  // prefix for AWS storage folder to make a valid URL.
  private static final String AWS_BUCKET_URL_PREFIX = "s3://";
  private static final Logger logger = LoggerFactory.getLogger(AwsStorageFolder.class);
  private final String bucketName;
  private final String prefix;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsStorageFolder(PDAwsStorageFolder configFromDisk) {
    super(configFromDisk);
    this.bucketName = configFromDisk.bucketName;
    this.prefix = configFromDisk.prefix;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsStorageFolder(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_STORAGE_FOLDER;
    this.bucketName = wsmObject.getResourceAttributes().getAwsStorageFolder().getBucketName();
    this.prefix = wsmObject.getResourceAttributes().getAwsStorageFolder().getPrefix();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsStorageFolder(AwsStorageFolderResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_STORAGE_FOLDER;
    this.bucketName = wsmObject.getAttributes().getBucketName();
    this.prefix = wsmObject.getAttributes().getPrefix();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsStorageFolder serializeToCommand() {
    return new UFAwsStorageFolder(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsStorageFolder serializeToDisk() {
    return new PDAwsStorageFolder(this);
  }

  /**
   * Add a AWS storage folder as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static AwsStorageFolder addReferenced(CreateAwsStorageFolderParams createParams) {
    throw new UserActionableException("Referenced resources not supported for AWS storage folder.");
  }

  /**
   * Create a AWS storage folder as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsStorageFolder createControlled(CreateAwsStorageFolderParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AwsStorageFolderResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledAwsStorageFolder(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created AWS storage folder: {}", createdResource);

    return new AwsStorageFolder(createdResource);
  }

  /** Delete a AWS storage folder referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException("Referenced resources not supported for AWS storage folder.");
  }

  /** Delete a AWS storage folder controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledAwsStorageFolder(Context.requireWorkspace().getUuid(), id);
  }

  /** Resolve a AWS storage folder resource to its cloud identifier. */
  public String resolve() {
    return resolve(true);
  }

  /**
   * Resolve a AWS storage folder resource to its cloud identifier. Optionally include the 's3://'
   * prefix.
   */
  public String resolve(boolean includeUrlPrefix) {
    return resolve(bucketName, prefix, includeUrlPrefix);
  }

  /**
   * Resolve a AWS storage folder resource to its cloud identifier. Optionally include the 's3://'
   * prefix.
   */
  public static String resolve(
      String awsBucketName, String awsBucketPrefix, boolean includeUrlPrefix) {
    String resolvedPath = String.format("%s/%s/", awsBucketName, awsBucketPrefix);
    return includeUrlPrefix ? AWS_BUCKET_URL_PREFIX + resolvedPath : resolvedPath;
  }

  /**
   * Returns the number of objects in the storage folder, up to the given limit, or null if there
   * was an error looking it up. This behavior is useful for display purposes.
   */
  public Integer numObjects(long limit) {
    // TODO(TERRA-146) implement numObjects
    return 0;
  }

  // ====================================================
  // Property getters.

  public String getBucketName() {
    return bucketName;
  }

  public String getPrefix() {
    return prefix;
  }
}
