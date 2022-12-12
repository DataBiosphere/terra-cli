package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalBQDatasets;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle controlled BQ datasets. */
@Tag("unit")
public class BqDatasetControlled extends SingleWorkspaceUnit {
  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Uses the
   * current workspace.
   */
  static UFBqDataset listOneDatasetResourceWithName(String resourceName)
      throws JsonProcessingException {
    return listOneDatasetResourceWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and expect one resource with this name. Filters on
   * the specified workspace id; Uses the current workspace if null.
   */
  static UFBqDataset listOneDatasetResourceWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    List<UFBqDataset> matchedResources =
        listDatasetResourcesWithName(resourceName, workspaceUserFacingId);

    assertEquals(1, matchedResources.size(), "found exactly one resource with this name");
    return matchedResources.get(0);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name. Uses the current workspace.
   */
  static List<UFBqDataset> listDatasetResourcesWithName(String resourceName)
      throws JsonProcessingException {
    return listDatasetResourcesWithName(resourceName, null);
  }

  /**
   * Helper method to call `terra resources list` and filter the results on the specified resource
   * name and workspace (uses the current workspace if null).
   */
  static List<UFBqDataset> listDatasetResourcesWithName(
      String resourceName, String workspaceUserFacingId) throws JsonProcessingException {
    // `terra resources list --type=BQ_DATASET --format=json`
    List<UFBqDataset> listedResources =
        workspaceUserFacingId == null
            ? TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {}, "resource", "list", "--type=BQ_DATASET")
            : TestCommand.runAndParseCommandExpectSuccess(
                new TypeReference<>() {},
                "resource",
                "list",
                "--type=BQ_DATASET",
                "--workspace=" + workspaceUserFacingId);

    // find the matching dataset in the list
    return listedResources.stream()
        .filter(resource -> resource.name.equals(resourceName))
        .collect(Collectors.toList());
  }

  @Test
  @DisplayName("list and describe reflect creating a new controlled dataset")
  void listDescribeReflectCreate() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "listDescribeReflectCreate";
    String datasetId = randomDatasetId();
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId);

    // check that the name, project id, and dataset id match
    assertEquals(name, createdDataset.name, "create output matches name");
    assertEquals(
        workspace.googleProjectId, createdDataset.projectId, "create output matches project id");
    assertEquals(datasetId, createdDataset.datasetId, "create output matches dataset id");

    // check that the dataset is in the list
    UFBqDataset matchedResource = listOneDatasetResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(datasetId, matchedResource.datasetId, "list output matches dataset id");

    // `terra resource describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // check that the name, project id, and dataset id match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        workspace.googleProjectId,
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(
        datasetId, describeResource.datasetId, "describe resource output matches dataset id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("create a new dataset but not specify dataset id")
  void createDatasetWithoutSpecifyingDatasetId() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "createDatasetWithoutSpecifyingDatasetId";
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "create", "bq-dataset", "--name=" + name);

    // check that the name, project id, and dataset id match
    assertEquals(name, createdDataset.name, "create output matches name");
    assertEquals(
        workspace.googleProjectId, createdDataset.projectId, "create output matches project id");
    assertEquals(
        name, createdDataset.datasetId, "reuse resource name because dataset-id is not specified");

    // check that the dataset is in the list
    UFBqDataset matchedResource = listOneDatasetResourceWithName(name);
    assertEquals(name, matchedResource.name, "list output matches name");
    assertEquals(name, matchedResource.datasetId, "list output matches dataset id");

    // `terra resource describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

    // check that the name, project id, and dataset id match
    assertEquals(name, describeResource.name, "describe resource output matches name");
    assertEquals(
        workspace.googleProjectId,
        describeResource.projectId,
        "describe resource output matches project id");
    assertEquals(name, describeResource.datasetId, "describe resource output matches dataset id");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("list reflects deleting a controlled dataset")
  void listReflectsDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "listReflectsDelete";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId);

    // `terra resource delete --name=$name --format=json`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");

    // check that the dataset is not in the list
    List<UFBqDataset> matchedResources = listDatasetResourcesWithName(name);
    assertEquals(0, matchedResources.size(), "no resource found with this name");
  }

  @Test
  @DisplayName("resolve a controlled dataset")
  void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "resolve";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId);

    // `terra resource resolve --name=$name --format=json`
    JSONObject resolved =
        TestCommand.runAndGetJsonObjectExpectSuccess("resource", "resolve", "--name=" + name);
    assertEquals(
        workspace.googleProjectId + "." + datasetId,
        resolved.get(name),
        "default resolve includes [project id].[dataset id]");

    // `terra resource resolve --name=$name --bq-path=PROJECT_ID_ONLY --format=json`
    JSONObject resolvedProjectIdOnly =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + name, "--bq-path=PROJECT_ID_ONLY");
    assertEquals(
        workspace.googleProjectId,
        resolvedProjectIdOnly.get(name),
        "resolve with option PROJECT_ID_ONLY only includes the project id");

    // `terra resource resolve --name=$name --bq-path=DATASET_ID_ONLY --format=json`
    JSONObject resolvedDatasetIdOnly =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resource", "resolve", "--name=" + name, "--bq-path=DATASET_ID_ONLY");
    assertEquals(
        datasetId,
        resolvedDatasetIdOnly.get(name),
        "resolve with option DATASET_ID_ONLY only includes the project id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check-access for a controlled dataset")
  void checkAccess() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "checkAccess";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "bq-dataset", "--name=" + name, "--dataset-id=" + datasetId);

    // `terra resources check-access --name=$name`
    String stdErr =
        TestCommand.runCommandExpectExitCode(1, "resource", "check-access", "--name=" + name);
    assertThat(
        "error message includes wrong stewardship type",
        stdErr,
        CoreMatchers.containsString("Checking access is intended for REFERENCED resources only"));

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("create a controlled dataset, specifying all options")
  void createWithAllOptions() throws IOException, InterruptedException {
    workspaceCreator.login();

    // `terra workspace set --id=$id --format=json`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --access=$access
    // --cloning=$cloning --description=$description --location=$location --format=json`
    String name = "createWithAllOptions";
    String datasetId = randomDatasetId();
    AccessScope access = AccessScope.PRIVATE_ACCESS;
    CloningInstructionsEnum cloning = CloningInstructionsEnum.DEFINITION;
    String description = "\"create with all options\"";
    String location = "us-east1";
    UFBqDataset createdDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId,
            "--access=" + access,
            "--cloning=" + cloning,
            "--description=" + description,
            "--location=" + location);

    // check that the properties match
    assertEquals(name, createdDataset.name, "create output matches name");
    assertEquals(
        workspace.googleProjectId, createdDataset.projectId, "create output matches project id");
    assertEquals(datasetId, createdDataset.datasetId, "create output matches dataset id");
    assertEquals(access, createdDataset.accessScope, "create output matches access");
    assertEquals(cloning, createdDataset.cloningInstructions, "create output matches cloning");
    assertEquals(description, createdDataset.description, "create output matches description");
    assertEquals(
        workspaceCreator.email.toLowerCase(),
        createdDataset.privateUserName.toLowerCase(),
        "create output matches private user name");

    Dataset createdDatasetOnCloud =
        CrlUtils.callGcpWithRetries(
            () ->
                ExternalBQDatasets.getBQClient(
                        workspaceCreator.getCredentialsWithCloudPlatformScope())
                    .getDataset(DatasetId.of(workspace.googleProjectId, datasetId)));
    assertNotNull(createdDatasetOnCloud, "looking up dataset via BQ API succeeded");
    assertEquals(
        location, createdDatasetOnCloud.getLocation(), "dataset location matches create input");

    // `terra resources describe --name=$name --format=json`
    UFBqDataset describeResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + name);

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

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("update a controlled dataset, one property at a time")
  void updateIndividualProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId
    // --description=$description`
    String name = "updateIndividualProperties";
    String description = "updateDescription";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "bq-dataset",
        "--name=" + name,
        "--description=" + description,
        "--dataset-id=" + datasetId);

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

    // update just the description
    // `terra resources update bq-dataset --name=$newName --new-description=$newDescription`
    // --new-cloning=$CloningInstructionsEnum.DEFINITION`
    String newDescription = "updateDescription_NEW";
    updatedDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + newName,
            "--new-description=" + newDescription,
            "--new-cloning=" + CloningInstructionsEnum.DEFINITION);
    assertEquals(newName, updatedDataset.name);
    assertEquals(newDescription, updatedDataset.description);
    assertEquals(CloningInstructionsEnum.DEFINITION, updatedDataset.cloningInstructions);
  }

  @Test
  @DisplayName("update a controlled dataset, specifying multiple properties")
  void updateMultipleProperties() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId
    // --description=$description`
    String name = "updateMultipleProperties";
    String description = "updateDescription";
    String datasetId = randomDatasetId();
    TestCommand.runCommandExpectSuccess(
        "resource",
        "create",
        "bq-dataset",
        "--name=" + name,
        "--description=" + description,
        "--dataset-id=" + datasetId);

    // update both the name and description
    // `terra resources update bq-dataset --name=$newName --new-name=$newName
    // --new-description=$newDescription`
    String newName = "updateMultipleProperties_NEW";
    String newDescription = "updateDescription_NEW";
    UFBqDataset updateDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "update",
            "bq-dataset",
            "--name=" + name,
            "--new-name=" + newName,
            "--new-description=" + newDescription);
    assertEquals(newName, updateDataset.name);
    assertEquals(newDescription, updateDataset.description);

    // `terra resources describe --name=$newName`
    UFBqDataset describeDataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class, "resource", "describe", "--name=" + newName);
    assertEquals(newDescription, describeDataset.description);
  }
}
