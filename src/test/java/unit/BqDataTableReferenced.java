package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
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
  @DisplayName("list and describe reflect adding a new referenced dataset")
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

    // check that the name, project id, and dataset id match
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
    assertEquals(
        externalDataTableName,
        describeResource.dataTableId,
        "describe resource output matches data table id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
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
