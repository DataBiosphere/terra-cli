package bio.terra.cli.resources;

import bio.terra.cli.Context;
import bio.terra.cli.Resource;
import bio.terra.cli.User;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateGcsBucket;
import bio.terra.cli.serialization.disk.resources.DiskGcsBucket;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsBucket extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucket.class);

  private String bucketName;

  // prefix for GCS bucket to make a valid URL.
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  protected GcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GcsBucket addReferenced(CreateUpdateGcsBucket createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpGcsBucketResource addedResource =
        new WorkspaceManagerService()
            .createReferencedGcsBucket(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket: {}", addedResource);

    // convert the WSM object to a CLI object
    listAndSync();
    return new Builder(addedResource).build();
  }

  /**
   * Create a GCS bucket as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static GcsBucket createControlled(CreateUpdateGcsBucket createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpGcsBucketResource createdResource =
        new WorkspaceManagerService()
            .createControlledGcsBucket(Context.requireWorkspace().getId(), createParams);
    logger.info("Created GCS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    listAndSync();
    return new Builder(createdResource).build();
  }

  /** Delete a GCS bucket referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    new WorkspaceManagerService().deleteReferencedGcsBucket(Context.requireWorkspace().getId(), id);
  }

  /** Delete a GCS bucket controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    new WorkspaceManagerService().deleteControlledGcsBucket(Context.requireWorkspace().getId(), id);
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

  /**
   * Builder class to help construct an immutable Resource object with lots of properties.
   * Sub-classes extend this with resource type-specific properties.
   */
  public static class Builder extends Resource.Builder {
    private String bucketName;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Method that returns the resource type. Should be hard-coded in sub-classes. */
    public ResourceType getResourceType() {
      return ResourceType.GCS_BUCKET;
    }

    /** Call the sub-class constructor. */
    public GcsBucket build() {
      return new GcsBucket(this);
    }

    /**
     * Populate this Builder object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to GCS buckets only.
     */
    public Builder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      this.bucketName = wsmObject.getResourceAttributes().getGcpGcsBucket().getBucketName();
    }

    /** Populate this Builder object with properties from the WSM GcpGcsBucketResource object. */
    public Builder(GcpGcsBucketResource wsmObject) {
      super(wsmObject.getMetadata());
      this.bucketName = wsmObject.getAttributes().getBucketName();
    }

    /**
     * Populate this Builder object with properties from the on-disk object. This method handles the
     * fields that apply to all resource types.
     */
    public Builder(DiskGcsBucket configFromDisk) {
      super(configFromDisk);
      this.bucketName = configFromDisk.bucketName;
    }
  }
}
