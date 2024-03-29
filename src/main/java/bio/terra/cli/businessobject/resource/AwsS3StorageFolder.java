package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDAwsS3StorageFolder;
import bio.terra.cli.serialization.userfacing.input.CreateAwsS3StorageFolderParams;
import bio.terra.cli.serialization.userfacing.resource.UFAwsS3StorageFolder;
import bio.terra.cli.service.WorkspaceManagerServiceAws;
import bio.terra.cli.utils.AwsConfiguration;
import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsS3StorageFolderResource;
import bio.terra.workspace.model.ResourceDescription;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a AWS S3 Storage Folder resource. Instances of this class are part of
 * the current context or state.
 */
public class AwsS3StorageFolder extends Resource {
  // prefix for AWS S3 Storage Folder to make a valid URL.
  private static final String S3_BUCKET_URL_PREFIX = "s3://";
  private static final Logger logger = LoggerFactory.getLogger(AwsS3StorageFolder.class);
  private final String bucketName;
  private final String prefix;

  /** Deserialize an instance of the disk format to the internal object. */
  public AwsS3StorageFolder(PDAwsS3StorageFolder configFromDisk) {
    super(configFromDisk);
    this.resourceType = Type.AWS_S3_STORAGE_FOLDER;
    this.bucketName = configFromDisk.bucketName;
    this.prefix = configFromDisk.prefix;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AwsS3StorageFolder(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_S3_STORAGE_FOLDER;
    this.bucketName = wsmObject.getResourceAttributes().getAwsS3StorageFolder().getBucketName();
    this.prefix = wsmObject.getResourceAttributes().getAwsS3StorageFolder().getPrefix();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AwsS3StorageFolder(AwsS3StorageFolderResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AWS_S3_STORAGE_FOLDER;
    this.bucketName = wsmObject.getAttributes().getBucketName();
    this.prefix = wsmObject.getAttributes().getPrefix();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAwsS3StorageFolder serializeToCommand() {
    return new UFAwsS3StorageFolder(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAwsS3StorageFolder serializeToDisk() {
    return new PDAwsS3StorageFolder(this);
  }

  /**
   * Add a AWS S3 Storage Folder as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static AwsS3StorageFolder addReferenced(CreateAwsS3StorageFolderParams createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for AWS S3 Storage Folder.");
  }

  /**
   * Create a AWS S3 Storage Folder as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AwsS3StorageFolder createControlled(CreateAwsS3StorageFolderParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    Workspace workspace = Context.requireWorkspace();

    // call WSM to create the resource
    AwsS3StorageFolderResource createdResource =
        WorkspaceManagerServiceAws.fromContext()
            .createControlledAwsS3StorageFolder(workspace.getUuid(), createParams);
    logger.info("Created AWS S3 Storage Folder: {}", createdResource);

    AwsConfiguration awsConfiguration = AwsConfiguration.loadFromDisk(workspace.getUuid());
    awsConfiguration.addResource(
        createParams.resourceFields.name, createParams.region, Type.AWS_S3_STORAGE_FOLDER);
    awsConfiguration.storeToDisk();

    return new AwsS3StorageFolder(createdResource);
  }

  /** Delete a AWS S3 Storage Folder referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for AWS S3 Storage Folder.");
  }

  /** Delete a AWS S3 Storage Folder controlled resource in the workspace. */
  protected void deleteControlled() {
    Workspace workspace = Context.requireWorkspace();

    // call WSM to delete the resource
    WorkspaceManagerServiceAws.fromContext()
        .deleteControlledAwsS3StorageFolder(workspace.getUuid(), id);

    AwsConfiguration awsConfiguration = AwsConfiguration.loadFromDisk(workspace.getUuid());
    awsConfiguration.removeResource(getName());
    awsConfiguration.storeToDisk();
  }

  /** Resolve a AWS S3 Storage Folder resource to its cloud identifier. */
  public String resolve() {
    return resolve(true);
  }

  /**
   * Resolve a AWS S3 Storage Folder resource to its cloud identifier. Optionally include the
   * 's3://' prefix.
   */
  public String resolve(boolean includeUrlPrefix) {
    return resolve(bucketName, prefix, includeUrlPrefix);
  }

  /**
   * Resolve a AWS S3 Storage Folder resource to its cloud identifier. Optionally include the
   * 's3://' prefix.
   */
  public static String resolve(
      String awsBucketName, String awsBucketPrefix, boolean includeUrlPrefix) {
    String resolvedPath = String.format("%s/%s/", awsBucketName, awsBucketPrefix);
    return includeUrlPrefix ? S3_BUCKET_URL_PREFIX + resolvedPath : resolvedPath;
  }

  public Object getCredentials(CredentialsAccessScope scope, int duration) {
    // call WSM to get credentials
    return WorkspaceManagerServiceAws.fromContext()
        .getControlledAwsS3StorageFolderCredential(
            Context.requireWorkspace().getUuid(),
            id,
            scope == CredentialsAccessScope.READ_ONLY
                ? AwsCredentialAccessScope.READ_ONLY
                : AwsCredentialAccessScope.WRITE_READ,
            duration);
  }

  public URL getConsoleUrl(CredentialsAccessScope scope, int duration) {
    try {
      URL destinationUrl =
          new URIBuilder()
              .setScheme("https")
              .setHost("s3.console.aws.amazon.com")
              .setPath(String.format("s3/buckets/%s", bucketName))
              .setParameter("region", region)
              .setParameter("prefix", String.format("%s/", prefix))
              .build()
              .toURL();

      return WorkspaceManagerServiceAws.createConsoleUrl(
          (AwsCredential) getCredentials(scope, duration), duration, destinationUrl);

    } catch (URISyntaxException | MalformedURLException e) {
      throw new SystemException("Failed to create destination URL.", e);
    }
  }

  /**
   * Returns the number of objects in the storage folder, up to the given limit, or null if there
   * was an error looking it up. This behavior is useful for display purposes.
   */
  public Integer numObjects(long limit) {
    return WorkspaceManagerServiceAws.fromContext()
        .getNumObjects(Context.requireWorkspace().getUuid(), this, limit);
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
