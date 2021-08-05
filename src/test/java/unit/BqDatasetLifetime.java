package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/** Tests for specifying default lifetimes for controlled BQ Datasets. */
@Tag("unit")
public class BqDatasetLifetime extends SingleWorkspaceUnit {

  @Override
  @BeforeEach
  protected void setupEachTime() throws IOException {
    super.setupEachTime();
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
  }

  @AfterEach
  protected void cleanupEachTime(TestInfo testInfo) {

    // Assumption is that each test case will create a single dataset, named after the test method
    // itself.  This will clean up after the test case.

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + getDatasetName(testInfo), "--quiet");
  }

  @Test
  @DisplayName("create with default partition lifetime only")
  void createWithDefaultPartitionLifetime(TestInfo testInfo) throws IOException {

    Integer lifetime = 9600;

    UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            getDatasetName(testInfo), "--default-partition-lifetime=" + lifetime.toString());

    Map<DefaultLifetimeEntity, Integer> defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(lifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));
  }

  @Test
  @DisplayName("create with default table lifetime only")
  void createWithDefaultTableLifetime(TestInfo testInfo) throws IOException {

    Integer lifetime = 4800;

    UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            getDatasetName(testInfo), "--default-table-lifetime=" + lifetime.toString());

    Map<DefaultLifetimeEntity, Integer> defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(lifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));
  }

  @Test
  @DisplayName("create with both default lifetimes")
  void createWithBothDefaultLifetimes(TestInfo testInfo) throws IOException {

    Integer partitionLifetime = 9600;
    Integer tableLifetime = 4800;

    UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            getDatasetName(testInfo),
            "--default-partition-lifetime=" + partitionLifetime.toString(),
            "--default-table-lifetime=" + tableLifetime.toString());

    Map<DefaultLifetimeEntity, Integer> defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(partitionLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(tableLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));
  }

  @Test
  @DisplayName("create with no default lifetimes and test update calls")
  void updateDefaultLifetimes(TestInfo testInfo) throws IOException {
    final String name = getDatasetName(testInfo);
    final Integer partitionLifetime = 9600;
    final Integer tableLifetime = 4800;

    // Create a dataset with no lifetimes configured
    UFBqDataset bqDataset = createDatasetWithLDefaultLifetimes(name);

    Map<DefaultLifetimeEntity, Integer> defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));

    // Now update the dataset with a default partition lifetime only and make sure it is set and
    // table stays the same.

    updateDatasetWithDefaultLifetimes(
        name, "--default-partition-lifetime=" + partitionLifetime.toString());
    defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(partitionLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));

    // Now update the dataset with a default table lifetime only and make sure it is set and
    // partition stays the same.

    updateDatasetWithDefaultLifetimes(name, "--default-table-lifetime=" + tableLifetime.toString());
    defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(partitionLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(tableLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));

    // Now set both to zero in a single call and make sure both get cleared.

    updateDatasetWithDefaultLifetimes(
        name, "--default-partition-lifetime=0", "--default-table-lifetime=0");

    defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));

    // Test that flags can be passed along with generic controlled resource update flags by renaming
    // the dataset (then renaming it back) along with a default lifetime update.

    final String renamed = name + "Renamed";

    updateDatasetWithDefaultLifetimes(
        name,
        "--new-name=" + renamed,
        "--default-partition-lifetime=" + partitionLifetime.toString());
    defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(partitionLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(0, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));

    updateDatasetWithDefaultLifetimes(
        renamed, "--new-name=" + name, "--default-table-lifetime=" + tableLifetime.toString());
    defaultLifetimeMap = getDefaultLifetimes(bqDataset);
    assertEquals(partitionLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.PARTITION));
    assertEquals(tableLifetime, defaultLifetimeMap.get(DefaultLifetimeEntity.TABLE));
  }

  /**
   * Helper method to get the name of the dataset that should be created by the test method (derived
   * from the passed TestInfo instance).
   */
  private String getDatasetName(TestInfo testInfo) {
    return testInfo.getTestMethod().orElseThrow().getName();
  }

  /** Creates a BQ Dataset with the given name and any passed lifetime arguments. */
  private UFBqDataset createDatasetWithLDefaultLifetimes(
      String name, String... defaultLifetimeArguments) throws JsonProcessingException {

    String datasetId = randomDatasetId();

    List<String> arguments =
        new ArrayList<>(
            Arrays.asList(
                "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId));

    arguments.addAll(Arrays.asList(defaultLifetimeArguments));

    return TestCommand.runAndParseCommandExpectSuccess(
        UFBqDataset.class, arguments.toArray(new String[0]));
  }

  /** Updates a dataset with a given name using the passed arguments. */
  private void updateDatasetWithDefaultLifetimes(String name, String... defaultLifetimeArguments) {
    List<String> arguments =
        new ArrayList<>(Arrays.asList("resource", "update", "bq-dataset", "--name=" + name));

    arguments.addAll(Arrays.asList(defaultLifetimeArguments));
    TestCommand.runCommandExpectSuccess(arguments.toArray(new String[0]));
  }

  /** Enum to use as a key for a map of entity to lifetime values. */
  private enum DefaultLifetimeEntity {
    PARTITION,
    TABLE
  }

  /**
   * Use the Google BQ Client to get the configured default lifetime for the passed dataset
   * descriptor and return it as a map of entity type (partition or table) to its configured default
   * lifetime in seconds.
   */
  private Map<DefaultLifetimeEntity, Integer> getDefaultLifetimes(UFBqDataset bqDataset)
      throws IOException {

    DatasetId datasetId = DatasetId.of(bqDataset.projectId, bqDataset.datasetId);

    Dataset dataset =
        ExternalBQDatasets.getBQClient(workspaceCreator.getCredentialsWithCloudPlatformScope())
            .getDataset(datasetId);

    assertNotNull(dataset, "looking up dataset via BQ API succeeded");

    // These methods return null if lifetime is not set, if they are set they need to be converted
    // to integers representing seconds to be returned in the map, with 0 representing no lifetime
    // set.  Both of these methods return milliseconds.
    Long defaultPartitionExpirationMs = dataset.getDefaultPartitionExpirationMs();
    Long defaultTableLifetime = dataset.getDefaultTableLifetime();

    return Map.of(
        DefaultLifetimeEntity.PARTITION,
        defaultPartitionExpirationMs == null ? 0 : defaultPartitionExpirationMs.intValue() / 1000,
        DefaultLifetimeEntity.TABLE,
        defaultTableLifetime == null ? 0 : defaultTableLifetime.intValue() / 1000);
  }
}
