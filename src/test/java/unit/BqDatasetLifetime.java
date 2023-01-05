package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.service.utils.CrlUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/** Tests for specifying default lifetimes for controlled BQ Datasets. */
@Tag("unit")
public class BqDatasetLifetime extends SingleWorkspaceUnit {

  /** Helper to covert a Duration in seconds to a String */
  private static String toSecondsString(Duration duration) {
    return Long.valueOf(duration.toSeconds()).toString();
  }

  @Override
  @BeforeEach
  protected void setupEachTime(TestInfo testInfo) throws IOException {
    super.setupEachTime(testInfo);
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
  }

  @Test
  @DisplayName("create with default partition lifetime only")
  void createWithDefaultPartitionLifetime(TestInfo testInfo)
      throws IOException, InterruptedException {

    final String name = testInfo.getTestMethod().orElseThrow().getName();
    final Duration lifetime = Duration.ofSeconds(9600);

    final UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            name, "--default-partition-lifetime=" + toSecondsString(lifetime));

    validateDefaultLifetimes(bqDataset, lifetime.toMillis(), null);
  }

  @Test
  @DisplayName("create with default table lifetime only")
  void createWithDefaultTableLifetime(TestInfo testInfo) throws IOException, InterruptedException {

    final String name = testInfo.getTestMethod().orElseThrow().getName();
    final Duration lifetime = Duration.ofHours(2);

    final UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            name, "--default-table-lifetime=" + toSecondsString(lifetime));

    validateDefaultLifetimes(bqDataset, null, lifetime.toMillis());
  }

  @Test
  @DisplayName("create with both default lifetimes")
  void createWithBothDefaultLifetimes(TestInfo testInfo) throws IOException, InterruptedException {
    final String name = testInfo.getTestMethod().orElseThrow().getName();
    final Duration partitionLifetime = Duration.ofSeconds(9600);
    final Duration tableLifetime = Duration.ofSeconds(4800);

    final UFBqDataset bqDataset =
        createDatasetWithLDefaultLifetimes(
            name,
            "--default-partition-lifetime=" + toSecondsString(partitionLifetime),
            "--default-table-lifetime=" + toSecondsString(tableLifetime));

    validateDefaultLifetimes(bqDataset, partitionLifetime.toMillis(), tableLifetime.toMillis());
  }

  @Test
  @DisplayName("create with no default lifetimes and test update calls")
  void updateDefaultLifetimes(TestInfo testInfo) throws IOException, InterruptedException {
    final String name = testInfo.getTestMethod().orElseThrow().getName();
    final Duration partitionLifetime = Duration.ofSeconds(9600);
    final Duration tableLifetime = Duration.ofSeconds(4800);

    // Create a dataset with no lifetimes configured
    final UFBqDataset bqDataset = createDatasetWithLDefaultLifetimes(name);
    validateDefaultLifetimes(bqDataset, null, null);

    // Now update the dataset with a default partition lifetime only and make sure it is set and
    // table stays the same.

    updateDatasetWithDefaultLifetimes(
        name, "--default-partition-lifetime=" + toSecondsString(partitionLifetime));

    validateDefaultLifetimes(bqDataset, partitionLifetime.toMillis(), null);

    // Now update the dataset with a default table lifetime only and make sure it is set and
    // partition stays the same.

    updateDatasetWithDefaultLifetimes(
        name, "--default-table-lifetime=" + toSecondsString(tableLifetime));
    validateDefaultLifetimes(bqDataset, partitionLifetime.toMillis(), tableLifetime.toMillis());

    // Now set both to zero in a single call and make sure both get cleared.

    updateDatasetWithDefaultLifetimes(
        name, "--default-partition-lifetime=0", "--default-table-lifetime=0");

    validateDefaultLifetimes(bqDataset, null, null);

    // Test that flags can be passed along with generic controlled resource update flags by renaming
    // the dataset (then renaming it back) along with a default lifetime update.

    final String renamed = name + "Renamed";

    updateDatasetWithDefaultLifetimes(
        name,
        "--new-name=" + renamed,
        "--default-partition-lifetime=" + toSecondsString(partitionLifetime));
    validateDefaultLifetimes(bqDataset, partitionLifetime.toMillis(), null);

    updateDatasetWithDefaultLifetimes(
        renamed,
        "--new-name=" + name,
        "--default-table-lifetime=" + toSecondsString(tableLifetime));

    validateDefaultLifetimes(bqDataset, partitionLifetime.toMillis(), tableLifetime.toMillis());
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
  private void updateDatasetWithDefaultLifetimes(String name, String... defaultLifetimeArguments)
      throws JsonProcessingException {
    List<String> arguments =
        new ArrayList<>(Arrays.asList("resource", "update", "bq-dataset", "--name=" + name));

    arguments.addAll(Arrays.asList(defaultLifetimeArguments));
    TestCommand.runCommandExpectSuccess(arguments.toArray(new String[0]));
  }

  /**
   * Use the Google BQ Client to validate the expected partition and table lifetimes for a dataset.
   *
   * @param bqDataset user facing representation of dataset to check
   * @param defaultPartitionLifetimeMilliseconds expected lifetime for partitions in milliseconds,
   *     or null if lifetime expected to be unset
   * @param defaultTableLifetimeMilliseconds expected lifetime for partitions in milliseconds, or
   *     null if lifetime expected to be unset
   * @throws IOException
   */
  private void validateDefaultLifetimes(
      UFBqDataset bqDataset,
      @Nullable Long defaultPartitionLifetimeMilliseconds,
      @Nullable Long defaultTableLifetimeMilliseconds)
      throws IOException, InterruptedException {

    DatasetId datasetId = DatasetId.of(bqDataset.projectId, bqDataset.datasetId);

    Dataset dataset =
        CrlUtils.callGcpWithRetries(
            () ->
                ExternalBQDatasets.getBQClient(
                        workspaceCreator.getCredentialsWithCloudPlatformScope())
                    .getDataset(datasetId));

    assertNotNull(dataset, "looking up dataset via BQ API succeeded");
    assertEquals(dataset.getDefaultPartitionExpirationMs(), defaultPartitionLifetimeMilliseconds);
    assertEquals(dataset.getDefaultTableLifetime(), defaultTableLifetimeMilliseconds);
  }
}
