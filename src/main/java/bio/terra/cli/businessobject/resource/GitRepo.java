package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDGitRepo;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGitRepoParams;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GitRepoResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a git repo workspace referenced resource. Instances of this class are
 * part of the current context or state.
 */
public class GitRepo extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(GitRepo.class);

  private String gitRepoUrl;

  /** Deserialize an instance of the disk format to the internal object. */
  public GitRepo(PDGitRepo configFromDisk) {
    super(configFromDisk);
    this.gitRepoUrl = configFromDisk.gitRepoUrl;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public GitRepo(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GIT_REPO;
    this.gitRepoUrl = wsmObject.getResourceAttributes().getGitRepo().getGitRepoUrl();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public GitRepo(GitRepoResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.GIT_REPO;
    this.gitRepoUrl = wsmObject.getAttributes().getGitRepoUrl();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGitRepo serializeToCommand() {
    return new UFGitRepo(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGitRepo serializeToDisk() {
    return new PDGitRepo(this);
  }

  /**
   * Add a git repo as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static GitRepo addReferenced(AddGitRepoParams addGitRepoParams) {
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
    return new GitRepo(addedResource);
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

  /** Delete a Git repo referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedGitRepo(Context.requireWorkspace().getId(), id);
  }

  @Override
  protected void deleteControlled() {
    logger.warn("Controlled git repo resource is not supported in workspace.");
  }

  /** Resolve git repo resource. */
  public String resolve() {
    return gitRepoUrl;
  }

  // ====================================================
  // Property getters.

  public String getGitRepoUrl() {
    return gitRepoUrl;
  }
}
