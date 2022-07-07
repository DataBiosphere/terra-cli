package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static unit.BqDatasetControlled.listDatasetResourcesWithName;
import static unit.BqDatasetControlled.listOneDatasetResourceWithName;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import harness.utils.ExternalBQDatasets.IamMemberType;
import java.io.IOException;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle referenced BQ datasets. */
@Tag("unit")
public class BqDatasetReferenced extends SingleWorkspaceUnit {

  // external dataset to use for creating BQ dataset references in the workspace
  private DatasetReference externalDataset;
  private DatasetReference externalDataset2;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalDataset = ExternalBQDatasets.createDataset();
    externalDataset2 = ExternalBQDatasets.createDataset();

    // grant the user's proxy group access to the dataset so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalBQDatasets.grantReadAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);
    ExternalBQDatasets.grantReadAccess(
        externalDataset2, Auth.getProxyGroupEmail(), IamMemberType.GROUP);
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalBQDatasets.deleteDataset(externalDataset);
    ExternalBQDatasets.deleteDataset(externalDataset2);
    externalDataset = null;
    externalDataset2 = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced dataset")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "listDescribeReflectAdd";
    UFBqDataset addedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "add-ref",
            "bq-dataset",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId());

    // check that the name, project id, and dataset id match
    assertEquals(name, addedDataset.name, "add ref output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        addedDataset.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        addedDataset.datasetId,
        "add ref output matches dataset id");

    // check that the dataset is in the list
    UFBqDataset matchedResource = listOneDatasetResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        matchedResource.projectId,
        "list output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        matchedResource.datasetId,
        "list output matches dataset id");

    // `terra resource describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // check that the name, project id, and dataset id match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        describeResource.datasetId,
        "describe resource output matches dataset id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced dataset")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "listReflectsDelete";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the dataset is not in the list
    List<UFBqDataset> matchedResources = listDatasetResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced dataset")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "resolve";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        new JSONObject(
            TestCommand.runAndGetStdoutExpectSuccess("resource", "resolve", "--name=" + name));
    assertEquals(
        ExternalBQDatasets.getDatasetFullPath(
            externalDataset.getProjectId(), externalDataset.getDatasetId()),
        resolved.get(name),
        "default resolve includes [project id].[dataset id]");

    // `terra resource resolve --name=$name --bq-path=PROJECT_ID_ONLY --format=json`
    JSONObject resolvedProjectIdOnly =
        new JSONObject(
            TestCommand.runAndGetStdoutExpectSuccess(
                "resource", "resolve", "--name=" + name, "--bq-path=PROJECT_ID_ONLY"));
    assertEquals(
        externalDataset.getProjectId(),
        resolvedProjectIdOnly.get(name),
        "resolve with option PROJECT_ID_ONLY only includes the project id");

    // `terra resource resolve --name=$name --bq-path=DATASET_ID_ONLY --format=json`
    JSONObject resolvedDatasetIdOnly =
        new JSONObject(
            TestCommand.runAndGetStdoutExpectSuccess(
                "resource", "resolve", "--name=" + name, "--bq-path=DATASET_ID_ONLY"));
    assertEquals(
        externalDataset.getDatasetId(),
        resolvedDatasetIdOnly.get(name),
        "resolve with option DATASET_ID_ONLY only includes the project id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a referenced dataset")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --format=json`
    String name = "checkAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // `terra resource check-access --name=$name
    TestCommand.runCommandExpectSuccess("resource", "check-access", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add a referenced dataset, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --cloning=$cloning
    // --description=$description --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;
    String description = "add with all options";
    UFBqDataset addedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "add-ref",
            "bq-dataset",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--cloning=" + cloning,
            "--description=" + description);

    // check that the properties match
    assertEquals(name, addedDataset.name, "add ref output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        addedDataset.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        addedDataset.datasetId,
        "add ref output matches dataset id");
    assertEquals(cloning, addedDataset.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, addedDataset.description, "add ref output matches description");

    // `terra resource describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        describeResource.datasetId,
        "describe resource output matches dataset id");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a referenced dataset, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId  --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--description=" + description,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // update just the name
    // `terra resources update bq-dataset --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFBqDataset updatedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updatedDataset.name);
    assertEquals(description, updatedDataset.description);

    // `terra resources describe --name=$newName`
    UFBqDataset describedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describedDataset.description);

    // update description and cloning instructions
    // `terra resources update bq-dataset --name=$newName --new-description=$newDescription
    // --new-cloning=$CloningInstructionsEnum.NOTHING`
    String newDescription = "updateDescription_NEW";
    updatedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + newName,
            "--new-description=" + newDescription,
            "--new-cloning=" + CloningInstructionsEnum.NOTHING);
    assertEquals(newName, updatedDataset.name);
    assertEquals(newDescription, updatedDataset.description);

    // `terra resources describe --name=$newName`
    describedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describedDataset.description);
    assertEquals(CloningInstructionsEnum.NOTHING, describedDataset.cloningInstructions);

    updatedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + newName,
            "--new-dataset-id=" + externalDataset2.getDatasetId());
    assertEquals(externalDataset2.getDatasetId(), updatedDataset.datasetId);
    assertEquals(externalDataset2.getProjectId(), updatedDataset.projectId);

    describedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describedDataset.description);
    assertEquals(externalDataset2.getProjectId(), describedDataset.projectId);
    assertEquals(externalDataset2.getDatasetId(), describedDataset.datasetId);
  }

  @Test
  @DisplayName("update a referenced dataset, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String controlledDataset = "controlledDataset";
    String datasetId = randomDatasetId();
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=" + controlledDataset,
            "--dataset-id=" + datasetId);

    // `terra resources add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId  --description=$description`
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--description=" + description,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // call update without specifying any properties to modify
    // `terra resources update bq-dataset --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(
            1, "resource", "update", "bq-dataset", "--name=" + name);
    assertThat(
        "error message says that at least one property must be specified",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update"));

    // update both the name and description
    // `terra resources update bq-dataset --name=$newName --new-name=$newName
    // --new-description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFBqDataset updateDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription,
            "--new-project-id=" + createdDataset.projectId,
            "--new-dataset-id=" + createdDataset.datasetId);
    assertEquals(newName, updateDataset.name);
    assertEquals(newDescription, updateDataset.description);
    assertEquals(createdDataset.projectId, updateDataset.projectId);
    assertEquals(createdDataset.datasetId, updateDataset.datasetId);

    // `terra resources describe --name=$newName`
    UFBqDataset describeDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeDataset.description);
    assertEquals(createdDataset.datasetId, describeDataset.datasetId);
    assertEquals(createdDataset.projectId, describeDataset.projectId);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + controlledDataset, "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }
}
