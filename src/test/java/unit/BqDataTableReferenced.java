package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BqDataTableReferenced extends SingleWorkspaceUnit {

  DatasetReference externalDataset;
  // name of table in external dataset
  private String externalDataTableName = "testTable";

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalDataset = ExternalBQDatasets.createDataset();

    // grant the user's proxy group access to the dataset so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalBQDatasets.grantWriteAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);

    // create a table in the dataset
    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalDataset.getProjectId(),
        externalDataset.getDatasetId(),
        externalDataTableName);
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalBQDatasets.deleteDataset(externalDataset);
    externalDataset = null;
  }

  @Test
  @DisplayName("list and describe reflect adding a new referenced data table")
  void listDescribeReflectAdd() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "listDescribeReflectAdd";
    UFBqDataTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "add-ref",
            "bq-table",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--table-id=" + externalDataTableName);

    // check that the name, project id, dataset id and table id match
    assertEquals(name, addedDataTable.name, "add ref output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        addedDataTable.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        addedDataTable.datasetId,
        "add ref output matches dataset id");
    assertEquals(
        externalDataTableName, addedDataTable.dataTableId, "add ref output matches data table id");

    // check that the data table is in the list
    List<UFBqDataTable> matchedResourceList = listDataTableResourcesWithName(name);
    assertEquals(1, matchedResourceList.size(), "Only 1 data table in the list");
    UFBqDataTable matchedResource = matchedResourceList.get(0);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        matchedResource.projectId,
        "list output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        matchedResource.datasetId,
        "list output matches dataset id");
    assertEquals(
        externalDataTableName, matchedResource.dataTableId, "List output matches data table id");

    // `terra resource describe --name=$name --format=json`
    UFBqDataTable describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class, "resource", "describe", "--name=" + name);

    // check that the name, project id, dataset id and table id match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        describeResource.datasetId,
        "describe resource output matches dataset id");
    assertEquals(
        externalDataTableName,
        describeResource.dataTableId,
        "describe resource output matches data table id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a referenced data table")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "listReflectsDelete";
    UFBqDataTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "add-ref",
            "bq-table",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--table-id=" + externalDataTableName);

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the data table is not in the list
    List<UFBqDataTable> matchedResources = listDataTableResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced data table")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "resolve";
    UFBqDataTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "add-ref",
            "bq-table",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--table-id=" + externalDataTableName);

    // `terra resource resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name);
    assertEquals(
        ExternalBQDatasets.getDataTablePath(
            externalDataset.getProjectId(), externalDataset.getDatasetId(), externalDataTableName),
        resolved,
        "default resolve include full path");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a referenced data table")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "checkAccess";
    UFBqDataTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "add-ref",
            "bq-table",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--table-id=" + externalDataTableName);

    // `terra resource check-access --name=$name
    TestCommand.runCommandExpectSuccess("resource", "check-access", "--name=" + name);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("add a referenced data table, specifying all options")
  void addWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --cloning=$cloning
    // --description=$description --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;
    String description = "add with all options";
    UFBqDataTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "add-ref",
            "bq-table",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId(),
            "--table-id=" + externalDataTableName,
            "--cloning=" + cloning,
            "--description=" + description);

    // check that the properties match
    assertEquals(name, addedDataTable.name, "add ref output matches name");
    assertEquals(
        externalDataset.getProjectId(),
        addedDataTable.projectId,
        "add ref output matches project id");
    assertEquals(
        externalDataset.getDatasetId(),
        addedDataTable.datasetId,
        "add ref output matches dataset id");
    assertEquals(
        externalDataTableName, addedDataTable.dataTableId, "add ref output matches data table id");
    assertEquals(cloning, addedDataTable.cloningInstructions, "add ref output matches cloning");
    assertEquals(description, addedDataTable.description, "add ref output matches description");

    // `terra resource describe --name=$name --format=json`
    UFBqDataTable describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class, "resource", "describe", "--name=" + name);

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
    assertEquals(
        externalDataTableName,
        describeResource.dataTableId,
        "describe resource output matches data table id");
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId  --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + name,
        "--description=" + description,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + externalDataTableName);

    // update just the name
    // `terra resources update bq-table --name=$name --new-name=$newName`
    String newName = "updateIndividualProperties_NEW";
    UFBqDataTable updateDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updateDataTable.name);
    assertEquals(description, updateDataTable.description);

    // `terra resources describe --name=$newName`
    UFBqDataTable describeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describeDataTable.description);

    // update just the description
    // `terra resources update bq-table --name=$newName --description=$newDescription`
    String newDescription = "updateDescription_NEW";
    updateDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + newName,
            "--description=" + newDescription);
    assertEquals(newName, updateDataTable.name);
    assertEquals(newDescription, updateDataTable.description);

    // `terra resources describe --name=$newName`
    describeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeDataTable.description);
  }

  @Test
  @DisplayName("update a referenced dataset, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId  --description=$description`
    String name = "updateMultipleOrNoProperties";
    String description = "updateDescription";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + name,
        "--description=" + description,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + externalDataTableName);

    // call update without specifying any properties to modify
    // `terra resources update bq-table --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "update", "bq-table", "--name=" + name);
    assertThat(
        "error message says that at least one property must be specified",
        stdErr,
        CoreMatchers.containsString("Specify at least one property to update"));

    // update both the name and description
    // `terra resources update bq-table --name=$newName --new-name=$newName
    // --description=$newDescription`
    String newName = "updateMultipleOrNoProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFBqDataTable updateDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + name,
            "--new-name=" + newName,
            "--description=" + newDescription);
    assertEquals(newName, updateDataTable.name);
    assertEquals(newDescription, updateDataTable.description);

    // `terra resources describe --name=$newName`
    UFBqDataTable describeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeDataTable.description);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFBqDataTable> listDataTableResourcesWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=BQ_DATA_TABLE --format=json`
    List<UFBqDataTable> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=BQ_DATA_TABLE");

    // find the matching data table in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
