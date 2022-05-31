package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BqTableReferenced extends SingleWorkspaceUnit {

  DatasetReference externalDataset;
  DatasetReference externalDataset2;
  // name of table in external dataset
  private String externalDataTableName = "testTable";
  private String externalDataTableName2 = "testTable2";

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalDataset = ExternalBQDatasets.createDataset();
    externalDataset2 = ExternalBQDatasets.createDataset();

    // grant the user's proxy group access to the dataset so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalBQDatasets.grantWriteAccess(
        externalDataset, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);
    ExternalBQDatasets.grantWriteAccess(
        externalDataset2, Auth.getProxyGroupEmail(), ExternalBQDatasets.IamMemberType.GROUP);

    // create a table in the dataset
    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalDataset.getProjectId(),
        externalDataset.getDatasetId(),
        externalDataTableName);

    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalDataset.getProjectId(),
        externalDataset.getDatasetId(),
        externalDataTableName2);

    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalDataset2.getProjectId(),
        externalDataset2.getDatasetId(),
        externalDataTableName2);
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "listDescribeReflectAdd";
    UFBqTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
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
    List<UFBqTable> matchedResourceList = listDataTableResourcesWithName(name);
    assertEquals(1, matchedResourceList.size(), "Only 1 data table in the list");
    UFBqTable matchedResource = matchedResourceList.get(0);
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
    UFBqTable describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + name);

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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "listReflectsDelete";
    TestCommand.runAndParseCommandExpectSuccess(
        UFBqTable.class,
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
    List<UFBqTable> matchedResources = listDataTableResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a referenced data table")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "resolve";
    TestCommand.runAndParseCommandExpectSuccess(
        UFBqTable.class,
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
        ExternalBQDatasets.getDataTableFullPath(
            externalDataset.getProjectId(), externalDataset.getDatasetId(), externalDataTableName),
        resolved,
        "default resolve include full path");

    // `terra resource resolve --name=$name --bq-path=PROJECT_ID_ONLY --format=json`
    String resolvedProjectIdOnly =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name, "--bq-path=PROJECT_ID_ONLY");
    assertEquals(
        externalDataset.getProjectId(),
        resolvedProjectIdOnly,
        "resolve with option PROJECT_ID_ONLY only includes the project id");

    // `terra resource resolve --name=$name --bq-path=DATASET_ID_ONLY --format=json`
    String resolvedDatasetIdOnly =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name, "--bq-path=DATASET_ID_ONLY");
    assertEquals(
        externalDataset.getDatasetId(),
        resolvedDatasetIdOnly,
        "resolve with option DATASET_ID_ONLY only includes the project id");

    String resolveTableIdOnly =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resource", "resolve", "--name=" + name, "--bq-path=TABLE_ID_ONLY");
    assertEquals(
        externalDataTableName,
        resolveTableIdOnly,
        "resolve with option TABLE_ID_ONLY only includes the table id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a referenced data table")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --table-id=$dataTableId --format=json`
    String name = "checkAccess";
    TestCommand.runAndParseCommandExpectSuccess(
        UFBqTable.class,
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId --cloning=$cloning
    // --description=$description --format=json`
    String name = "addWithAllOptions";
    CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;
    String description = "add with all options";
    UFBqTable addedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
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
    UFBqTable describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + name);

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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

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
    UFBqTable updatedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updatedDataTable.name);
    assertEquals(description, updatedDataTable.description);

    // `terra resources describe --name=$newName`
    UFBqTable describedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(description, describedDataTable.description);

    // update just the description
    // `terra resources update bq-table --name=$newName --description=$newDescription
    // --new-cloning=COPY_DEFINITION`
    String newDescription = "updateDescription_NEW";
    updatedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + newName,
            "--description=" + newDescription,
            "--new-cloning=" + CloningInstructionsEnum.DEFINITION);
    assertEquals(newName, updatedDataTable.name);
    assertEquals(newDescription, updatedDataTable.description);

    // `terra resources describe --name=$newName`
    describedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describedDataTable.description);
    assertEquals(CloningInstructionsEnum.DEFINITION, describedDataTable.cloningInstructions);

    updatedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + newName,
            "--new-table-id=" + externalDataTableName2);
    assertEquals(externalDataTableName2, updatedDataTable.dataTableId);
    assertEquals(externalDataset.getDatasetId(), updatedDataTable.datasetId);
    assertEquals(externalDataset.getProjectId(), updatedDataTable.projectId);

    updatedDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + newName,
            "--new-dataset-id=" + externalDataset2.getDatasetId());
    assertEquals(externalDataTableName2, updatedDataTable.dataTableId);
    assertEquals(externalDataset2.getDatasetId(), updatedDataTable.datasetId);
    assertEquals(externalDataset2.getProjectId(), updatedDataTable.projectId);
  }

  @Test
  @DisplayName("update a referenced data table, specifying multiple or none of the properties")
  void updateMultipleOrNoProperties() throws IOException, InterruptedException {
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
    String tableInControlledDataset = "tableInControlledDataset";
    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        createdDataset.projectId,
        createdDataset.datasetId,
        tableInControlledDataset);

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
    UFBqTable updateDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + name,
            "--new-name=" + newName,
            "--description=" + newDescription,
            "--new-project-id=" + createdDataset.projectId,
            "--new-dataset-id=" + createdDataset.datasetId,
            "--new-table-id=" + tableInControlledDataset);
    assertEquals(newName, updateDataTable.name);
    assertEquals(newDescription, updateDataTable.description);
    assertEquals(createdDataset.datasetId, updateDataTable.datasetId);
    assertEquals(createdDataset.projectId, updateDataTable.projectId);
    assertEquals(tableInControlledDataset, updateDataTable.dataTableId);

    // `terra resources describe --name=$newName`
    UFBqTable describeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeDataTable.description);
    assertEquals(newName, describeDataTable.name);
    assertEquals(createdDataset.projectId, describeDataTable.projectId);
    assertEquals(createdDataset.datasetId, describeDataTable.datasetId);
    assertEquals(tableInControlledDataset, describeDataTable.dataTableId);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + controlledDataset, "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
  }

  @Test
  @DisplayName("referenced dataset with no access does not fail the describe command")
  void numRowsForReferencedWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource add-ref bq-table --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    String name = "numRowsForReferencedWithNoAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + externalDataTableName);

    // `terra resource describe --name=$name`
    UFBqTable describeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + name);

    assertEquals(
        BigInteger.ZERO,
        describeDataTable.numRows,
        "referenced dataset with access and has zero row.");

    // `terra workspace add-user --email=$email --role=READER`
    TestUser shareeUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

    // `terra resource describe --name=$name`
    UFBqTable shareeDescribeDataTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class, "resource", "describe", "--name=" + name);

    // the external dataset created in the beforeall method should have 1 table in it, but the
    // sharee user doesn't have read access to the table so they can't know that
    assertNull(
        shareeDescribeDataTable.numRows, "referenced dataset with no access contains NULL numRows");
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name on current workspace.
   */
  private static List<UFBqTable> listDataTableResourcesWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=BQ_TABLE --format=json`
    List<UFBqTable> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=BQ_TABLE");

    // find the matching data table in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
