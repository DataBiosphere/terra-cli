package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static unit.BqDatasetControlled.listDatasetResourceWithName;
import static unit.BqDatasetControlled.listOneDatasetResourceWithName;

import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.google.cloud.bigquery.Dataset;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resources` commands that handle referenced BQ datasets. */
@Tag("unit")
public class BqDatasetReferenced extends SingleWorkspaceUnit {

  // external dataset to use for creating BQ dataset references in the workspace
  private Dataset externalDataset;

  @BeforeEach
  @Override
  protected void setupEachTime() throws IOException {
    super.setupEachTime();
    externalDataset = ExternalBQDatasets.createDataset();
    ExternalBQDatasets.grantReadAccess(externalDataset, workspaceCreator.email);
  }

  @AfterEach
  protected void cleanupEachTime() throws IOException {
    ExternalBQDatasets.deleteDataset(externalDataset);
    externalDataset = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced dataset")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "listDescribeReflectAdd";
    UFBqDataset addedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resources",
            "add-ref",
            "bq-dataset",
            "--name=" + name,
            "--project-id=" + externalDataset.getDatasetId().getProject(),
            "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
            "--format=json");

    // check that the name, project id, and dataset id match
    assertEquals(name, addedDataset.name, "add ref output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        addedDataset.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        addedDataset.datasetId,
        "add ref output matches dataset id");

    // check that the dataset is in the list
    UFBqDataset matchedResource = listOneDatasetResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        matchedResource.projectId,
        "list output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        matchedResource.datasetId,
        "list output matches dataset id");

    // `terra resources describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the name, project id, and dataset id match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        describeResource.datasetId,
        "describe resource output matches dataset id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("list reflects deleting a referenced dataset")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "listReflectsDelete";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getDatasetId().getProject(),
        "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
        "--format=json");

    // `terra resources delete --name=$name --format=json`
    UFBqDataset deletedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "delete", "--name=" + name, "--format=json");

    // check that the name, project id, and dataset id match
    assertEquals(name, deletedDataset.name, "delete output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        deletedDataset.projectId,
        "delete output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        deletedDataset.datasetId,
        "delete output matches dataset id");

    // check that the dataset is not in the list
    List<UFBqDataset> matchedResources = listDatasetResourceWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced dataset")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "resolve";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getDatasetId().getProject(),
        "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
        "--format=json");

    // `terra resources resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name, "--format=json");
    assertEquals(
        externalDataset.getDatasetId().getProject()
            + "."
            + externalDataset.getDatasetId().getDataset(),
        resolved,
        "default resolve includes [project id].[dataset id]");

    // `terra resources resolve --name=$name --bq-path=PROJECT_ID_ONLY --format=json`
    String resolvedProjectIdOnly =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class,
            "resources",
            "resolve",
            "--name=" + name,
            "--bq-path=PROJECT_ID_ONLY",
            "--format=json");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        resolvedProjectIdOnly,
        "resolve with option PROJECT_ID_ONLY only includes the project id");

    // `terra resources resolve --name=$name --bq-path=DATASET_ID_ONLY --format=json`
    String resolvedDatasetIdOnly =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class,
            "resources",
            "resolve",
            "--name=" + name,
            "--bq-path=DATASET_ID_ONLY",
            "--format=json");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        resolvedDatasetIdOnly,
        "resolve with option DATASET_ID_ONLY only includes the project id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("check-access for a referenced dataset")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "checkAccess";
    TestCommand.runCommandExpectSuccess(
        "resources",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getDatasetId().getProject(),
        "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
        "--format=json");

    // `terra resources check-access --name=$name
    TestCommand.runCommandExpectSuccess("resources", "check-access", "--name=" + name);

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("add a referenced dataset, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --cloning=$cloning
    // --description=$description --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;
    String description = "add with all options";
    UFBqDataset addedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resources",
            "add-ref",
            "bq-dataset",
            "--name=" + name,
            "--project-id=" + externalDataset.getDatasetId().getProject(),
            "--dataset-id=" + externalDataset.getDatasetId().getDataset(),
            "--cloning=" + cloning,
            "--description=" + description,
            "--format=json");

    // check that the properties match
    assertEquals(name, addedDataset.name, "add ref output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        addedDataset.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        addedDataset.datasetId,
        "add ref output matches dataset id");
    assertEquals(cloning, addedDataset.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, addedDataset.description, "add ref output matches description");

    // `terra resources describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalDataset.getDatasetId().getProject(),
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        externalDataset.getDatasetId().getDataset(),
        describeResource.datasetId,
        "describe resource output matches dataset id");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }
}
