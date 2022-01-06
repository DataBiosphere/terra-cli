package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.Test;

public class BqTableReferencedUpdate extends SingleWorkspaceUnit {

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

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    // `terra workspace add-user --email=$email --role=WRITER`
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=WRITER");

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
  @DisplayName("Attempt to update table reference but the user has no access.")
  void updateTableReferenceWithNoAccess() throws IOException {
    workspaceCreator.login();

    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    String newName = "updateTableReferenceWithNoAccess_NEW";
    TestCommand.runCommandExpectExitCode(
        2, "resource", "update", "bq-table", "--name=" + name, "--new-name=" + newName);

    // Clean up.
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName(
      "Attempt to update table reference when the user only have access to sharedExternalTable.")
  void updateTableReferenceWithPartialAccess() throws IOException {
    workspaceCreator.login();

    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

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
  }

  @Test
  @DisplayName(
      "Attempt to add a reference to tables when the user only has access to sharedExternalTable.")
  void addTableReferenceWithPartialAccess() throws IOException {
    shareeUser.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

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
  }
}
