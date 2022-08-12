package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.app.CommandRunner;
import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.serialization.userfacing.resource.UFDataCollection;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.WorkspaceManagerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.MoreCollectors;
import harness.TestCommand;
import harness.TestCommand.Result;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import harness.utils.TestUtils;
import harness.utils.WorkspaceUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle data collections. */
@Tag("unit")
public class DataCollectionReferenced extends SingleWorkspaceUnit {
  private static final String DATA_COLLECTION_NAME = "1000 Genomes";
  private static final String DATA_COLLECTION_DESCRIPTION = "short description";
  private static final String DATA_COLLECTION_VERSION = "version";
  private static final String DATA_COLLECTION_RESOURCE_NAME =
      TestUtils.appendRandomNumber("1000-genomes-ref");
  private static final String DATA_COLLECTION_BUCKET_NAME = "bucket-name";
  private static final String DATA_COLLECTION_BUCKET_RESOURCE_NAME =
      TestUtils.appendRandomNumber("bucket_resource_name");
  private static final String GIT_REPO_URL =
      "https://github.com/DataBiosphere/terra-workspace-manager.git";
  private static final String DATA_COLLECTION_GIT_RESOURCE_NAME =
      TestUtils.appendRandomNumber("git_resource_name");

  private UUID dataCollectionUuid;

  @BeforeEach
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();

    // Set up data collection workspace
    String properties =
        String.format(
            "%s=%s,%s=%s",
            DataCollection.SHORT_DESCRIPTION_KEY,
            DATA_COLLECTION_DESCRIPTION,
            DataCollection.VERSION_KEY,
            DATA_COLLECTION_VERSION);
    String thousandGenomesUfId =
        WorkspaceUtils.createWorkspace(
                workspaceCreator, DATA_COLLECTION_NAME, /*description=*/ "", properties)
            .id;
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + DATA_COLLECTION_BUCKET_RESOURCE_NAME,
        "--bucket-name=" + DATA_COLLECTION_BUCKET_NAME,
        "--workspace=" + thousandGenomesUfId);
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + DATA_COLLECTION_GIT_RESOURCE_NAME,
        "--repo-url=" + GIT_REPO_URL,
        "--workspace=" + thousandGenomesUfId);

    // Add a data collection to researcher workspace. We don't support add-ref for data
    // collections (see PF-1742), so call WSM directly.
    UUID researcherWorkspaceUuid =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(getUserFacingId()).getId();
    dataCollectionUuid =
        WorkspaceManagerService.fromContext()
            .getWorkspaceByUserFacingId(thousandGenomesUfId)
            .getId();
    WorkspaceManagerService.fromContext()
        .createReferencedTerraWorkspace(
            researcherWorkspaceUuid, dataCollectionUuid, DATA_COLLECTION_RESOURCE_NAME);
    System.out.println(
        "Created data collection resource with name " + DATA_COLLECTION_RESOURCE_NAME);
  }

  @Test
  void listDescribeDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource list --type=DATA_COLLECTION`
    List<UFDataCollection> actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_COLLECTION");

    // Assert resource list result
    UFDataCollection actual =
        actualResources.stream()
            .filter(resource -> resource.name.equals(DATA_COLLECTION_RESOURCE_NAME))
            .collect(MoreCollectors.onlyElement());
    assertDataCollectionResource(actual);

    // `terra resource describe --name=$name`
    actual =
        TestCommand.runAndParseCommandExpectSuccess(
            UFDataCollection.class,
            "resource",
            "describe",
            "--name=" + DATA_COLLECTION_RESOURCE_NAME);

    // Assert resource describe result
    assertDataCollectionResource(actual);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + DATA_COLLECTION_RESOURCE_NAME, "--quiet");

    // Assert resource was deleted
    actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_COLLECTION");
    actualResources.stream()
        .noneMatch(resource -> resource.name.equals(DATA_COLLECTION_RESOURCE_NAME));
  }

  @Test
  public void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resolve --name=$DATA_COLLECTION_RECOLLECTION_NAME`
    JSONObject resolve =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + DATA_COLLECTION_RESOURCE_NAME);
    assertDataCollectionResourceResolve(resolve);

    // `terra resolve --name=$DATA_COLLECTION_RECOLLECTION_NAME/`
    JSONObject resolve2 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + DATA_COLLECTION_RESOURCE_NAME + "/");
    assertDataCollectionResourceResolve(resolve2);

    // `terra resolve
    // --name=$DATA_COLLECTION_RECOLLECTION_NAME/$DATA_COLLECTION_BUCKET_RECOLLECTION_NAME`
    JSONObject resolve3 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve",
            "--name=" + DATA_COLLECTION_RESOURCE_NAME + "/" + DATA_COLLECTION_BUCKET_RESOURCE_NAME);
    assertEquals(
        ExternalGCSBuckets.getGsPath(DATA_COLLECTION_BUCKET_NAME),
        resolve3.get(DATA_COLLECTION_BUCKET_RESOURCE_NAME));
    assertEquals(1, resolve3.length());

    // `terra resolve
    // --name=$DATA_COLLECTION_RECOLLECTION_NAME/$DATA_COLLECTION_BUCKET_RECOLLECTION_NAME/random`
    var err =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name="
                + DATA_COLLECTION_RESOURCE_NAME
                + "/"
                + DATA_COLLECTION_BUCKET_RESOURCE_NAME
                + "/"
                + RandomStringUtils.random(10));
    assertTrue(err.contains("Invalid path"));

    // `terra resolve --name=$DATA_COLLECTION_RECOLLECTION_NAME/<random>`
    var err2 =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name=" + DATA_COLLECTION_RESOURCE_NAME + "/" + RandomStringUtils.random(10));
    assertTrue(err2.contains("Invalid path"));
  }

  @Test
  @DisplayName("Test running terra app execute env in a workspace with data collection")
  void appExecuteEnv() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra app execute env`
    Result result = TestCommand.runAndGetResultExpectSuccess("app", "execute", "env");
    String stdOut = result.stdOut;
    assertDataCollectionResourcesEnv(stdOut);
  }

  @Test
  public void gitClone() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra git clone --all`
    TestCommand.runCommandExpectSuccess("git", "clone", "--all");

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-workspace-manager", ".git")));

    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-workspace-manager"));
  }

  private void assertDataCollectionResource(UFDataCollection actual) {
    // For some reason resourceType is null, so don't assert resourceType.
    assertEquals(dataCollectionUuid, actual.dataCollectionWorkspaceUuid);
    assertEquals(DATA_COLLECTION_NAME, actual.title);
    assertEquals(DATA_COLLECTION_DESCRIPTION, actual.shortDescription);
    assertEquals(DATA_COLLECTION_VERSION, actual.version);
    assertNotNull(actual.createdDate);
    assertNotNull(actual.lastUpdatedDate);

    UFGcsBucket actualBucket = (UFGcsBucket) actual.resources.get(0);
    assertEquals(DATA_COLLECTION_BUCKET_RESOURCE_NAME, actualBucket.name);
    assertEquals(DATA_COLLECTION_BUCKET_NAME, actualBucket.bucketName);
  }

  private void assertDataCollectionResourceResolve(JSONObject resolve) {
    assertEquals(
        ExternalGCSBuckets.getGsPath(DATA_COLLECTION_BUCKET_NAME),
        resolve.get(DATA_COLLECTION_BUCKET_RESOURCE_NAME));
    assertEquals(GIT_REPO_URL, resolve.get(DATA_COLLECTION_GIT_RESOURCE_NAME));
    assertEquals(2, resolve.length());
  }

  private void assertDataCollectionResourcesEnv(String stdOut) {
    String expectedBucketEnvVariable =
        CommandRunner.convertToEnvironmentVariable(DATA_COLLECTION_RESOURCE_NAME)
            + "_"
            + DATA_COLLECTION_BUCKET_RESOURCE_NAME
            + "="
            + ExternalGCSBuckets.getGsPath(DATA_COLLECTION_BUCKET_NAME);
    assertTrue(
        stdOut.contains(expectedBucketEnvVariable),
        "Did not get expected bucket environment variable: " + expectedBucketEnvVariable);
    String expectedGitRepoEnvVariable =
        CommandRunner.convertToEnvironmentVariable(DATA_COLLECTION_RESOURCE_NAME)
            + "_"
            + DATA_COLLECTION_GIT_RESOURCE_NAME
            + "="
            + GIT_REPO_URL;
    assertTrue(
        stdOut.contains(expectedGitRepoEnvVariable),
        "Did not get expected git repo environment variable: " + expectedGitRepoEnvVariable);
  }
}
