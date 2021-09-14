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
import bio.terra.workspace.model.CloneResourceResult;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContextUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@Tag("unit")
public class CloneWorkspace extends ClearContextUnit {
  protected static final TestUsers workspaceCreator = TestUsers.chooseTestUserWithSpendAccess();
  private DatasetReference externalDataset;
  private UFWorkspace sourceWorkspace;
  private UFWorkspace destinationWorkspace;

  @BeforeEach
  @Override
  public void setupEachTime() throws IOException {
    super.setupEachTime();
  }

  @AfterEach
  public void cleanupEachTime() throws IOException {
    if (sourceWorkspace != null) {
      TestCommand.runCommandExpectSuccess(
          "workspace", "delete", "--quiet", "--workspace=" + sourceWorkspace.id);
    }
    if (destinationWorkspace != null) {
      TestCommand.runCommandExpectSuccess(
          "workspace", "delete", "--quiet", "--workspace=" + destinationWorkspace.id);
    }
    if (externalDataset != null) {
      ExternalBQDatasets.deleteDataset(externalDataset);
      externalDataset = null;
    }
  }

  @Test
  public void cloneWorkspace(TestInfo testInfo) throws Exception {
    workspaceCreator.login();

    // create a workspace
    // `terra workspace create --format=json`
    sourceWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");

    // Switch to the new workspace
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + sourceWorkspace.id);

    // Add a bucket resource
    UFGcsBucket sourceBucket =
        TestCommand.runAndParseCommandExpectSuccess(
            UFGcsBucket.class,
            "resource",
            "create",
            "gcs-bucket",
            "--name=" + "bucket_1",
            "--bucket-name=" + UUID.randomUUID(),
            "--cloning=COPY_RESOURCE");

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
            "--description=" + "\"The first dataset.\"",
            "--cloning=COPY_NOTHING");

    // Add a referenced resource
    externalDataset = ExternalBQDatasets.createDataset();
    ExternalBQDatasets.grantReadAccess(
        externalDataset, workspaceCreator.email, ExternalBQDatasets.IamMemberType.USER);

    // grant the user's proxy group access to the dataset so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalBQDatasets.grantReadAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);

    UFBqDataset datasetReference =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "add-ref",
            "bq-dataset",
            "--name=" + "dataset_ref",
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId());

    // Clone the workspace
    UFClonedWorkspace clonedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFClonedWorkspace.class,
            "workspace",
            "clone",
            "--name=" + "cloned_workspace",
            "--description=" + "\"A clone.\"");

    assertEquals(sourceWorkspace.id, clonedWorkspace.sourceWorkspace.id);
    destinationWorkspace = clonedWorkspace.destinationWorkspace;
    assertThat(clonedWorkspace.resources, hasSize(3));

    UFClonedResource bucketClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> sourceBucket.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(CloneResourceResult.SUCCEEDED, bucketClonedResource.result);
    assertNotNull(bucketClonedResource.destinationResource);

    UFClonedResource copyNothingBucketClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> copyNothingBucket.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(CloneResourceResult.SKIPPED, copyNothingBucketClonedResource.result);
    assertNull(copyNothingBucketClonedResource.destinationResource);

    UFClonedResource datasetRefClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> datasetReference.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(CloneResourceResult.SUCCEEDED, datasetRefClonedResource.result);
    assertEquals(
        StewardshipType.REFERENCED, datasetRefClonedResource.destinationResource.stewardshipType);

    UFClonedResource datasetClonedResource =
        getOrFail(
            clonedWorkspace.resources.stream()
                .filter(cr -> sourceDataset.id.equals(cr.sourceResource.id))
                .findFirst());
    assertEquals(CloneResourceResult.SUCCEEDED, datasetClonedResource.result);
    assertEquals("The first dataset.", datasetClonedResource.sourceResource.description);

    // Switch to the new workspace from the clone
    TestCommand.runCommandExpectSuccess(
        "workspace", "set", "--id=" + clonedWorkspace.destinationWorkspace.id);

    // Validate resources
    List<UFResource> resources =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "resource", "list");
    assertThat(resources, hasSize(2));
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
