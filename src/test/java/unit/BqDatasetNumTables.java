package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for including the number of tables in a BQ dataset resource's description. */
@Tag("unit")
public class BqDatasetNumTables extends SingleWorkspaceUnit {
  // external dataset to use for creating BQ dataset references in the workspace
  private DatasetReference externalDataset;

  // name of tables in external dataset
  private String privateExternalTable = "testTable";
  private String sharedExternalTable = "testTable2";

  private TestUsers shareeUser;

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
        privateExternalTable);
    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        externalDataset.getProjectId(),
        externalDataset.getDatasetId(),
        sharedExternalTable);

    shareeUser = TestUsers.chooseTestUserWhoIsNot(workspaceCreator);
    shareeUser.login();
    ExternalBQDatasets.grantReadAccessToTable(
        externalDataset.getProjectId(),
        externalDataset.getDatasetId(),
        sharedExternalTable,
        Auth.getProxyGroupEmail());
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalBQDatasets.deleteDataset(externalDataset);
    externalDataset = null;
  }

  @Test
  @DisplayName("controlled dataset displays the number of tables")
  void numTablesForControlled() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId`
    String name = "numTablesForControlled";
    String datasetId = randomDatasetId();
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId);

    // check that there are initially 0 tables reported in the dataset
    assertEquals(0, createdDataset.numTables, "created dataset contains 0 tables");

    // create a table in the dataset
    String tableName = "testTable";
    ExternalBQDatasets.createTable(
        workspaceCreator.getCredentialsWithCloudPlatformScope(),
        createdDataset.projectId,
        createdDataset.datasetId,
        tableName);

    // `terra resource describe --name=$name`
    UFBqDataset describedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // check that there is now 1 table reported in the dataset
    assertEquals(1, describedDataset.numTables, "described dataset contains 1 table");

    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("referenced dataset displays the number of tables")
  void numTablesForReferenced() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    String name = "numTablesForReferenced";
    UFBqDataset addedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "add-ref",
            "bq-dataset",
            "--name=" + name,
            "--project-id=" + externalDataset.getProjectId(),
            "--dataset-id=" + externalDataset.getDatasetId());

    // the external dataset created in the beforeall method should have 2 table in it
    assertEquals(2, addedDataset.numTables, "referenced dataset contains 2 table");

    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("referenced dataset with no access does not fail the describe command")
  void numTablesForReferencedWithNoAccess() throws IOException {
    workspaceCreator.login();

    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    String name = "numTablesForReferencedWithNoAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    shareeUser.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra resource describe --name=$name`
    UFBqDataset describeDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // the external dataset created in the beforeall method should have 1 table in it, but the
    // sharee user doesn't have read access to the dataset so they can't know that
    assertNull(describeDataset.numTables, "referenced dataset with no access contains NULL tables");

    // Clean up.
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  void updateTableReferenceWithNoAccess() throws IOException {
    workspaceCreator.login();

    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    String name = "updateTableReferenceWithNoAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + privateExternalTable);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    String newName = "updateTableReferenceWithNoAccess_NEW";
    TestCommand.runCommandExpectExitCode(
        2, "resource", "update", "bq-table", "--name=" + name, "--new-name=" + newName);

    // Clean up.
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  void updateTableReferenceWithPartialAccess() throws IOException {
    workspaceCreator.login();

    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    String name = "updateTableReferenceWithPartialAccess";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + name,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + sharedExternalTable);

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    String newName = "updateTableReferenceWithPartialAccess_NEW";
    UFBqTable updateTable =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqTable.class,
            "resource",
            "update",
            "bq-table",
            "--name=" + name,
            "--new-name=" + newName);
    assertEquals(newName, updateTable.name);

    TestCommand.runCommandExpectExitCode(
        1,
        "resource",
        "update",
        "bq-table",
        "--name=" + name,
        "--new-table-id=" + privateExternalTable);

    // clean up
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + newName, "--quiet");
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  void addTableReferenceWithPartialAccess() throws IOException {
    workspaceCreator.login();
    UFWorkspace createWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "create");
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    // `terra workspace add-user --email=$email --role=READER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);

    String succeedName = "addTableReferenceWithPartialAccess_withAccess";
    // `terra resource add-ref bq-dataset --name=$name --project-id=$projectId
    // --dataset-id=$datasetId`
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + succeedName,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + sharedExternalTable);

    String failureName = "addTableReferenceWithPartialAccess_withNoAccess";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "bq-table",
        "--name=" + failureName,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId(),
        "--table-id=" + privateExternalTable);

    String failureDatasetName = "addTableReferenceWithPartialAccess_withNoAccessToDataset";
    TestCommand.runCommandExpectExitCode(
        2,
        "resource",
        "add-ref",
        "bq-dataset",
        "--name=" + failureDatasetName,
        "--project-id=" + externalDataset.getProjectId(),
        "--dataset-id=" + externalDataset.getDatasetId());

    // clean up
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + succeedName, "--quiet");
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + createWorkspace.id);
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
