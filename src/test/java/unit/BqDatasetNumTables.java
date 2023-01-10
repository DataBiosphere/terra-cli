package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import com.google.api.services.bigquery.model.DatasetReference;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.Auth;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for including the number of tables in a BQ dataset resource's description. */
@Tag("unit-gcp")
public class BqDatasetNumTables extends SingleWorkspaceUnitGcp {
  // external dataset to use for creating BQ dataset references in the workspace
  private DatasetReference externalDataset;

  // name of tables in external dataset
  private final String externalTable = "testTable";

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
        externalTable);
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

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

    // the external dataset created in the beforeall method should have 1 table in it
    assertEquals(1, addedDataset.numTables, "referenced dataset contains 1 table");

    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("referenced dataset with no access does not fail the describe command")
  void numTablesForReferencedWithNoAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

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

    // `terra workspace add-user --email=$email --role=READER`
    TestUser shareeUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareeUser.email, "--role=READER");

    shareeUser.login();

    // `terra resource describe --name=$name`
    UFBqDataset describeDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // the external dataset created in the beforeall method should have 1 table in it, but the
    // sharee user doesn't have read access to the dataset so they can't know that
    assertNull(describeDataset.numTables, "referenced dataset with no access contains NULL tables");
  }
}
