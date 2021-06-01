package bio.terra.cli.context.resources;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonDeserialize(builder = GcsBucket.GcsBucketBuilder.class)
public class GcsBucket extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucket.class);

  public final String bucketName;
  public final GcsBucketLifecycle lifecycle;

  // storage class (https://cloud.google.com/storage/docs/storage-classes)
  public final GcpGcsBucketDefaultStorageClass defaultStorageClass;

  // bucket location (https://cloud.google.com/storage/docs/locations)
  public final String location;

  // prefix for GCS bucket to make a valid URL.
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  public GcsBucket(GcsBucketBuilder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
    this.location = builder.location;
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was created
   */
  protected GcsBucket addReferenced() {
    // call WSM to add the reference
    GcpGcsBucketResource createdResource =
        new WorkspaceManagerService()
            .createReferencedGcsBucket(GlobalContext.get().requireCurrentWorkspace().id, this);
    logger.info("Created GCS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    return new GcsBucketBuilder(createdResource).build();
  }

  /**
   * Create a GCS bucket as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  protected GcsBucket createControlled() {
    // call WSM to create the resource
    GcpGcsBucketResource createdResource =
        new WorkspaceManagerService()
            .createControlledGcsBucket(GlobalContext.get().requireCurrentWorkspace().id, this);
    logger.info("Created GCS bucket: {}", createdResource);

    // convert the WSM object to a CLI object
    return new GcsBucketBuilder(createdResource).build();
  }

  /** Delete a GCS bucket referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    new WorkspaceManagerService()
        .deleteReferencedGcsBucket(GlobalContext.get().requireCurrentWorkspace().id, resourceId);
  }

  /** Delete a GCS bucket controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    new WorkspaceManagerService()
        .deleteControlledGcsBucket(GlobalContext.get().requireCurrentWorkspace().id, resourceId);
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
    TerraUser currentUser = GlobalContext.get().requireCurrentTerraUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(CheckAccessCredentials.USER)
            ? currentUser.userCredentials
            : currentUser.petSACredentials;

    return new GoogleCloudStorage(
            credentials, GlobalContext.get().requireCurrentWorkspace().googleProjectId)
        .checkObjectsListAccess(resolve());
  }

  /** Print out a GCS bucket resource in text format. */
  public void printText() {
    super.printText();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCS bucket name: " + bucketName);
  }

  /** Builder class to help construct an immutable GcsBucket object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class GcsBucketBuilder extends ResourceBuilder {
    private String bucketName;
    private GcsBucketLifecycle lifecycle;
    private GcpGcsBucketDefaultStorageClass defaultStorageClass;
    private String location;

    public GcsBucketBuilder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public GcsBucketBuilder lifecycle(GcsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public GcsBucketBuilder defaultStorageClass(
        GcpGcsBucketDefaultStorageClass defaultStorageClass) {
      this.defaultStorageClass = defaultStorageClass;
      return this;
    }

    public GcsBucketBuilder location(String location) {
      this.location = location;
      return this;
    }

    /** Subclass-specific method that returns the resource type. */
    @JsonIgnore
    public ResourceType getResourceType() {
      return ResourceType.GCS_BUCKET;
    }

    /** Subclass-specific method that calls the sub-class constructor. */
    public GcsBucket build() {
      return new GcsBucket(this);
    }

    /** Default constructor for Jackson. */
    public GcsBucketBuilder() {
      super();
    }

    /**
     * Populate this Resource object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to GCS buckets only.
     */
    public GcsBucketBuilder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      this.bucketName = wsmObject.getResourceAttributes().getGcpGcsBucket().getBucketName();
    }

    /** Populate this Resource object with properties from the WSM GcpGcsBucketResource object. */
    public GcsBucketBuilder(GcpGcsBucketResource wsmObject) {
      super(wsmObject.getMetadata());
      this.bucketName = wsmObject.getAttributes().getBucketName();
    }
  }
}
