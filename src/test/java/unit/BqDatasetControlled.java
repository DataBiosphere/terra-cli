package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.IamRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.bigquery.Dataset;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resources` commands that handle controlled BQ datasets. */
@Tag("unit")
public class BqDatasetControlled extends SingleWorkspaceUnit {
  @Test
  @DisplayName("list and describe reflect creating a new controlled dataset")
  void listDescribeReflectCreate() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "listDescribeReflectCreate";
    String datasetId = randomDatasetId();
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resources",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId,
            "--format=json");

    // check that the name and dataset id match
    assertEquals(name, createdDataset.name, "create output matches name");
    assertEquals(datasetId, createdDataset.datasetId, "create output matches dataset id");

    // check that the dataset is in the list
    UFBqDataset matchedResource = listOneDatasetResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(datasetId, matchedResource.datasetId, "list output matches dataset id");

    // `terra resources describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the name and bucket name match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        datasetId, describeResource.datasetId, "describe resource output matches dataset id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("list reflects deleting a controlled dataset")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "listReflectsDelete";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "bq-dataset",
        "--name=" + name,
        "--dataset-id=" + datasetId,
        "--format=json");

    // `terra resources delete --name=$name --format=json`
    UFBqDataset deletedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "delete", "--name=" + name, "--format=json");

    // check that the name and bucket name match
    assertEquals(name, deletedDataset.name, "delete output matches name");
    assertEquals(datasetId, deletedDataset.datasetId, "delete output matches dataset id");

    // check that the bucket is not in the list
    List<UFBqDataset> matchedResources = listDatasetResourceWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a controlled dataset")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId(), "--format=json");

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "resolve";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "bq-dataset",
        "--name=" + name,
        "--dataset-id=" + datasetId,
        "--format=json");

    // `terra resources resolve --name=$name --format=json`
    String resolved =
        TestCommand.runAndParseCommandExpectSuccess(
            String.class, "resources", "resolve", "--name=" + name, "--format=json");
    assertEquals(
        workspace.googleProjectId + "." + datasetId,
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
        workspace.googleProjectId,
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
        datasetId,
        resolvedDatasetIdOnly,
        "resolve with option DATASET_ID_ONLY only includes the project id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("check-access for a controlled dataset")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "checkAccess";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resources",
        "create",
        "bq-dataset",
        "--name=" + name,
        "--dataset-id=" + datasetId,
        "--format=json");

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resources", "check-access", "--name=" + name);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("create a controlled dataset, specifying all options")
  void createWithAllOptions() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId(), "--format=json");

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --access=$access
    // --cloning=$cloning --description=$description --email=$email --iam-roles=$iamRole
    // --location=$location --format=json`
    String name = "createWithAllOptions";
    String datasetId = randomDatasetId();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.DEFINITION;
    String description = "\"create with all options\"";
    IamRole iamRole = IamRole.READER;
    String location = "us-east1";
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resources",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--email=" + workspaceCreator.email.toLowerCase(),
            "--iam-roles=" + iamRole,
            "--location=" + location,
            "--format=json");

    // check that the properties match
    assertEquals(name, createdDataset.name, "create output matches name");
    assertEquals(datasetId, createdDataset.datasetId, "create output matches dataset id");
    assertEquals(access, createdDataset.accessScope, "create output matches access");
    assertEquals(cloning, createdDataset.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdDataset.description, "create output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdDataset.privateUserName.toLowerCase(),
        "create output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them

    Dataset createdDatasetOnCloud =
        ExternalBQDatasets.getDataset(
            workspace.googleProjectId, datasetId, workspaceCreator.getCredentials());
    assertNotNull(createdDatasetOnCloud, "looking up dataset via BQ API succeeded");
    assertEquals(
        location, createdDatasetOnCloud.getLocation(), "dataset location matches create input");

    // `terra resources describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resources", "describe", "--name=" + name, "--format=json");

    // check that the properties match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        datasetId, describeResource.datasetId, "describe resource output matches dataset id");
    assertEquals(access, describeResource.accessScope, "describe output matches access");
    assertEquals(cloning, describeResource.cloningInstructions, "describe output matches cloning");
    assertEquals(description, describeResource.description, "describe output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        describeResource.privateUserName.toLowerCase(),
        "describe output matches private user name");
    // TODO (PF-616): check the private user roles once WSM returns them

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  /** Helper method to call `terra resources list` and expect one resource with this name. */
  static UFBqDataset listOneDatasetResourceWithName(String resourceName)
      throws JsonProcessingException {
    List<UFBqDataset> matchedResources = listDatasetResourceWithName(resourceName);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name.
   */
  static List<UFBqDataset> listDatasetResourceWithName(String resourceName)
      throws JsonProcessingException {
    // `terra resources list --type=BQ_DATASET --format=json`
    List<UFBqDataset> listedResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resources", "list", "--type=BQ_DATASET", "--format=json");

    // find the matching bucket in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }
}
