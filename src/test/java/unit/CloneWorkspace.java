package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFClonedResource;
import bio.terra.cli.serialization.userfacing.UFClonedWorkspace;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
import bio.terra.workspace.model.CloneResourceResult;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.TestContext;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
public class CloneWorkspace extends ClearContextUnit {
  private static final TestUser workspaceCreator = TestUser.chooseTestUserWithSpendAccess();
  private static final Logger logger = LoggerFactory.getLogger(CloneWorkspace.class);

  private static final String GIT_REPO_HTTPS_URL =
      "https://github.com/DataBiosphere/terra-workspace-manager.git";
  private static final String GIT_REPO_REF_NAME = "gitrepo_ref";
  private static final int SOURCE_RESOURCE_NUM = 5;
  private static final int DESTINATION_RESOURCE_NUM = 4;

  private static DatasetReference externalDataset;
  private UFWorkspace sourceWorkspace;
  private UFWorkspace destinationWorkspace;

  @BeforeAll
  public static void setupOnce() throws IOException {
    TestContext.clearGlobalContextDir();
    resetContext();

    workspaceCreator.login(); // login needed to get user's proxy group

    // create an external dataset to use for a referenced resource
    externalDataset = ExternalBQDatasets.createDataset();

    // grant the workspace creator access to the dataset
    ExternalBQDatasets.grantReadAccess(
        externalDataset, workspaceCreator.email, ExternalBQDatasets.IamMemberType.USER);

    // grant the user's proxy group access to the dataset so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalBQDatasets.grantReadAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);
  }

  @AfterEach
  public void cleanupEachTime() throws IOException {
    workspaceCreator.login();
    if (sourceWorkspace != null) {
      TestCommand.Result result =
          TestCommand.runCommand(
              "workspace", "delete", "--quiet", "--workspace=" + sourceWorkspace.id);
      sourceWorkspace = null;
      if (0 != result.exitCode) {
        logger.error("Failed to delete source workspace. exit code = {}", result.exitCode);
      }
    }
    if (destinationWorkspace != null) {
      TestCommand.Result result =
          TestCommand.runCommand(
              "workspace", "delete", "--quiet", "--workspace=" + destinationWorkspace.id);
      destinationWorkspace = null;
      if (0 != result.exitCode) {
        logger.error("Failed to delete destination workspace. exit code = {}", result.exitCode);
      }
    }
  }

  @AfterAll
  public static void cleanupOnce() throws IOException {
    if (externalDataset != null) {
      ExternalBQDatasets.deleteDataset(externalDataset);
      externalDataset = null;
    }
  }

  @Test
  public void cloneWorkspace(TestInfo testInfo) throws Exception {
    workspaceCreator.login();

    // create a workspace
    sourceWorkspace = WorkspaceUtils.createWorkspace(workspaceCreator);

    // create a radom workspace clone
    Random RANDOM = new Random();

    // Add a bucket resource
    UFGcsBucket sourceBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + "bucket_1",
            "--bucket-name=" + UUID.randomUUID()); // cloning defaults  to COPY_RESOURCE

    // Add another bucket resource with COPY_NOTHING
    UFGcsBucket copyNothingBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + "bucket_2",
            "--bucket-name=" + UUID.randomUUID(),
            "--cloning=COPY_NOTHING");

    // Add a dataset resource
    UFBqDataset sourceDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=dataset_1",
            "--dataset-id=dataset_1",
            "--description=The first dataset.",
            "--cloning=COPY_RESOURCE");

    UFBqDataset datasetReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "add-ref",
            "bq-dataset",
            "--name=dataset_ref",
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--cloning=COPY_REFERENCE");

    UFGitRepo gitRepositoryReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGitRepo.class,
            "resource",
            "add-ref",
            "git-repo",
            "--name=" + GIT_REPO_REF_NAME,
            "--repo-url=" + GIT_REPO_HTTPS_URL,
            "--cloning=COPY_REFERENCE");

    // Update workspace name. This is for testing PF-1623.
    TestCommand.runAndParseCommandExpectSuccess(
        UFWorkspace.class, "workspace", "update", "--new-name=update_name");

    // Clone the workspace
    UFClonedWorkspace clonedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFClonedWorkspace.class,
            "workspace",
            "clone",
            "--new-id=cloned_id" + RANDOM.nextInt(Integer.MAX_VALUE),
            "--name=cloned_workspace",
            "--description=A clone.",
            "--property=key1=value1,key2=value2");

    assertEquals(
        sourceWorkspace.id,
        clonedWorkspace.sourceWorkspace.id,
        "Correct source workspace ID for clone.");
    destinationWorkspace = clonedWorkspace.destinationWorkspace;
    assertEquals(
        2, destinationWorkspace.property.size(), "Multiple property entries add successful.");
    assertThat(
        "There are 5 cloned resources", clonedWorkspace.resources, hasSize(SOURCE_RESOURCE_NUM));

    UFClonedResource bucketClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> sourceBucket.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(
        CloneResourceResult.SUCCEEDED, bucketClonedResource.result, "bucket clone succeeded");
    assertNotNull(
        bucketClonedResource.destinationResource, "Destination bucket resource was created");

    UFClonedResource copyNothingBucketClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> copyNothingBucket.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(
        CloneResourceResult.SKIPPED,
        copyNothingBucketClonedResource.result,
        "COPY_NOTHING resource was skipped.");
    assertNull(
        copyNothingBucketClonedResource.destinationResource,
        "Skipped resource has no destination resource.");

    UFClonedResource datasetRefClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> datasetReference.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(
        CloneResourceResult.SUCCEEDED,
        datasetRefClonedResource.result,
        "Dataset reference clone succeeded.");
    assertEquals(
        StewardshipType.REFERENCED,
        datasetRefClonedResource.destinationResource.stewardshipType,
        "Dataset reference has correct stewardship type.");

    UFClonedResource datasetClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> sourceDataset.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(
        CloneResourceResult.SUCCEEDED, datasetClonedResource.result, "Dataset clone succeeded.");
    assertEquals(
        "The first dataset.",
        datasetClonedResource.destinationResource.description,
        "Dataset description matches.");

    UFClonedResource gitRepoClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> gitRepositoryReference.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(
        CloneResourceResult.SUCCEEDED, gitRepoClonedResource.result, "Git repo clone succeeded");
    assertEquals(
        GIT_REPO_REF_NAME,
        gitRepoClonedResource.destinationResource.name,
        "Resource type matches GIT_REPO");

    // Switch to the new workspace from the clone
    TestCommand.runCommandExpectSuccess(
        "workspace", "set", "--id=" + clonedWorkspace.destinationWorkspace.id);

    // Validate resources
    List<UFResource> resources =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "resource", "list");
    assertThat(
        "Destination workspace has three resources.", resources, hasSize(DESTINATION_RESOURCE_NUM));
  }

  @Test
  public void cloneFailsWithoutNewUserFacingId() throws IOException {
    workspaceCreator.login();

    // `terra workspace clone`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "workspace", "clone");
    assertThat(
        "error message indicate user must set ID",
        stdErr,
        CoreMatchers.containsString("Missing required option: '--new-id=<id>'"));
  }

  /**
   * Check Optional's value is present and return it, or else fail an assertion.
   *
   * @param optional - Optional expression
   * @param <T> - value type of optional
   * @return - value of optional, if present
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <T> T getOrFail(Optional<T> optional) {
    assertTrue(optional.isPresent(), "Optional value was empty.");
    return optional.get();
  }
}
