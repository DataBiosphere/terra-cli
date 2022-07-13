package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
import bio.terra.cli.service.WorkspaceManagerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.MoreCollectors;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import harness.utils.TestUtils;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle data sources. */
@Tag("unit")
public class DataSourceReferenced extends SingleWorkspaceUnit {
  private static final String DATA_SOURCE_NAME = "1000 Genomes";
  private static final String DATA_SOURCE_DESCRIPTION = "short description";
  private static final String DATA_SOURCE_VERSION = "version";
  private static final String DATA_SOURCE_RESOURCE_NAME =
      TestUtils.appendRandomNumber("1000-genomes-ref");
  private static final String DATA_SOURCE_BUCKET_NAME = "bucket-name";
  private static final String DATA_SOURCE_BUCKET_RESOURCE_NAME =
      TestUtils.appendRandomNumber("bucket_resource_name");
  private static final String GIT_REPO_SSH_URL =
      "git@github.com:DataBiosphere/terra-workspace-manager.git";
  private static final String DATA_SOURCE_GIT_RESOURCE_NAME =
      TestUtils.appendRandomNumber("git_resource_name");

  private UUID dataSourceUuid;

  @BeforeEach
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();

    // Set up data source workspace
    String properties =
        String.format(
            "%s=%s,%s=%s",
            DataSource.SHORT_DESCRIPTION_KEY,
            DATA_SOURCE_DESCRIPTION,
            DataSource.VERSION_KEY,
            DATA_SOURCE_VERSION);
    String thousandGenomesUfId =
        WorkspaceUtils.createWorkspace(
                workspaceCreator, DATA_SOURCE_NAME, /*description=*/ "", properties)
            .id;
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + DATA_SOURCE_BUCKET_RESOURCE_NAME,
        "--bucket-name=" + DATA_SOURCE_BUCKET_NAME,
        "--workspace=" + thousandGenomesUfId);
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + DATA_SOURCE_GIT_RESOURCE_NAME,
        "--repo-url=" + GIT_REPO_SSH_URL,
        "--workspace=" + thousandGenomesUfId);

    // Add a data source to researcher workspace. We don't support add-ref for data
    // sources (see PF-1742), so call WSM directly.
    UUID researcherWorkspaceUuid =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(getUserFacingId()).getId();
    dataSourceUuid =
        WorkspaceManagerService.fromContext()
            .getWorkspaceByUserFacingId(thousandGenomesUfId)
            .getId();
    WorkspaceManagerService.fromContext()
        .createReferencedTerraWorkspace(
            researcherWorkspaceUuid, dataSourceUuid, DATA_SOURCE_RESOURCE_NAME);
    System.out.println("Created data source resource with name " + DATA_SOURCE_RESOURCE_NAME);
  }

  @Test
  void listDescribeDelete() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource list --type=DATA_SOURCE`
    List<UFDataSource> actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_SOURCE");

    // Assert resource list result
    UFDataSource actual =
        actualResources.stream()
            .filter(resource -> resource.name.equals(DATA_SOURCE_RESOURCE_NAME))
            .collect(MoreCollectors.onlyElement());
    assertDataSourceResource(actual);

    // `terra resource describe --name=$name`
    actual =
        TestCommand.runAndParseCommandExpectSuccess(
            UFDataSource.class, "resource", "describe", "--name=" + DATA_SOURCE_RESOURCE_NAME);

    // Assert resource describe result
    assertDataSourceResource(actual);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + DATA_SOURCE_RESOURCE_NAME, "--quiet");

    // Assert resource was deleted
    actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_SOURCE");
    actualResources.stream().noneMatch(resource -> resource.name.equals(DATA_SOURCE_RESOURCE_NAME));
  }

  @Test
  public void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resolve --name=$DATA_SOURCE_RESOURCE_NAME`
    JSONObject resolve =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + DATA_SOURCE_RESOURCE_NAME);
    assertDataSourceResourceResolve(resolve);

    // `terra resolve --name=$DATA_SOURCE_RESOURCE_NAME/`
    JSONObject resolve2 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + DATA_SOURCE_RESOURCE_NAME + "/");
    assertDataSourceResourceResolve(resolve2);

    // `terra resolve --name=$DATA_SOURCE_RESOURCE_NAME/$DATA_SOURCE_BUCKET_RESOURCE_NAME`
    JSONObject resolve3 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve",
            "--name=" + DATA_SOURCE_RESOURCE_NAME + "/" + DATA_SOURCE_BUCKET_RESOURCE_NAME);
    assertEquals(
        ExternalGCSBuckets.getGsPath(DATA_SOURCE_BUCKET_NAME),
        resolve3.get(DATA_SOURCE_BUCKET_RESOURCE_NAME));
    assertEquals(1, resolve3.length());

    // `terra resolve
    // --name=$DATA_SOURCE_RESOURCE_NAME/$DATA_SOURCE_BUCKET_RESOURCE_NAME/random`
    var err =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name="
                + DATA_SOURCE_RESOURCE_NAME
                + "/"
                + DATA_SOURCE_BUCKET_RESOURCE_NAME
                + "/"
                + RandomStringUtils.random(10));
    assertTrue(err.contains("Invalid path"));

    // `terra resolve --name=$DATA_SOURCE_RESOURCE_NAME/<random>`
    var err2 =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name=" + DATA_SOURCE_RESOURCE_NAME + "/" + RandomStringUtils.random(10));
    assertTrue(err2.contains("Invalid path"));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + DATA_SOURCE_RESOURCE_NAME, "--quiet");
  }

  private void assertDataSourceResourceResolve(JSONObject resolve) {
    assertEquals(
        ExternalGCSBuckets.getGsPath(DATA_SOURCE_BUCKET_NAME),
        resolve.get(DATA_SOURCE_BUCKET_RESOURCE_NAME));
    assertEquals(GIT_REPO_SSH_URL, resolve.get(DATA_SOURCE_GIT_RESOURCE_NAME));
    assertEquals(2, resolve.length());
  }

  private void assertDataSourceResource(UFDataSource actual) {
    // For some reason resourceType is null, so don't assert resourceType.
    assertEquals(dataSourceUuid, actual.dataSourceWorkspaceUuid);
    assertEquals(DATA_SOURCE_NAME, actual.title);
    assertEquals(DATA_SOURCE_DESCRIPTION, actual.shortDescription);
    assertEquals(DATA_SOURCE_VERSION, actual.version);
  }
}
