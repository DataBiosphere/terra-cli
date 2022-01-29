package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDGcsBucket;
import bio.terra.cli.serialization.persisted.resource.PDGitRepository;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGitRepoParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepository;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.storage.BucketCow;
import bio.terra.cloudres.google.storage.StorageCow;
import bio.terra.workspace.model.CreateGitRepoReferenceRequestBody;
import bio.terra.workspace.model.GitRepoResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCS bucket workspace resource. Instances of this class are part of
 * the current context or state.
 */
public class GitRepository extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GcsBucket.class);

  private String gitRepoUrl;

  // prefix for GCS bucket to make a valid URL.
  protected static final String GCS_BUCKET_URL_PREFIX = "gs://";

  /** Deserialize an instance of the disk format to the internal object. */
  public GitRepository(PDGitRepository configFromDisk) {
    super(configFromDisk);
    this.gitRepoUrl = configFromDisk.gitRepoUrl;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public GitRepository(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GIT_REPO;
    this.gitRepoUrl = wsmObject.getResourceAttributes().getGitRepo().getGitRepoUrl();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public GitRepository(GitRepoResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GCS_BUCKET;
    this.gitRepoUrl = wsmObject.getAttributes().getGitRepoUrl();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGitRepository serializeToCommand() {
    return new UFGitRepository(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGitRepository serializeToDisk() {
    return new PDGitRepository(this);
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GitRepository addReferenced(AddGitRepoParams addGitRepoParams) {
    validateEnvironmentVariableName(addGitRepoParams.resourceFields.name);

    // call WSM to add the reference. use the pet SA credentials instead of the end user's
    // credentials, because they include the cloud-platform scope. WSM needs the cloud-platform
    // scope to perform its access check before adding the reference. note that this means a user
    // cannot add a reference unless their pet SA has access to it.
    GitRepoResource addedResource =
        WorkspaceManagerService.fromContextForPetSa()
            .createReferencedGitRepo(Context.requireWorkspace().getId(), addGitRepoParams);
    logger.info("Created Git repo reference: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new GitRepository(addedResource);
  }

  /** Update a Git repo referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedGitRepoParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateEnvironmentVariableName(updateParams.resourceFields.name);
    }
    if (updateParams.gitRepoUrl != null) {
      this.gitRepoUrl = updateParams.gitRepoUrl;
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedGitRepo(Context.requireWorkspace().getId(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
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
    return includePrefix ? GCS_BUCKET_URL_PREFIX + gitRepoUrl : gitRepoUrl;
  }

  /** Query the cloud for information about the bucket. */
  public Optional<BucketCow> getBucket() {
    try {
      StorageCow storageCow =
          CrlUtils.createStorageCow(Context.requireUser().getPetSACredentials());
      return Optional.of(storageCow.get(gitRepoUrl));
    } catch (Exception ex) {
      logger.error("Caught exception looking up bucket", ex);
      return Optional.empty();
    }
  }

  // ====================================================
  // Property getters.

  public String getGitRepoUrl() {
    return gitRepoUrl;
  }
}
