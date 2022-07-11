package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
import bio.terra.cli.serialization.userfacing.resource.UFGitRepo;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle data sources. */
@Tag("unit")
public class DataSourceReferenced extends SingleWorkspaceUnit {
  private static final String THOUSAND_GENOMES_WORKSPACE_NAME = "1000 Genomes";
  private static final String THOUSAND_GENOMES_SHORT_DESCRIPTION = "short description";
  private static final String THOUSAND_GENOMES_VERSION = "version";
  private static final String THOUSAND_GENOMES_RESOURCE_NAME =
      TestUtils.appendRandomNumber("1000-genomes-ref");
  private static final String THOUSAND_GENOMES_BUCKET_NAME = "bucket-name";
  private static final String THOUSAND_GENOMES_BUCKET_RESOURCE_NAME =
      TestUtils.appendRandomNumber("bucket_resource_name");
  private static final String GIT_REPO_SSH_URL =
      "git@github.com:DataBiosphere/terra-workspace-manager.git";
  private static final String THOUSAND_GENOMES_GIT_RESOURCE_NAME =
      TestUtils.appendRandomNumber("git_resource_name");

  private UUID thousandGenomesUuid;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();

    // Set up 1000 Genomes data source workspace
    String properties =
        String.format(
            "%s=%s,%s=%s",
            DataSource.SHORT_DESCRIPTION_KEY,
            THOUSAND_GENOMES_SHORT_DESCRIPTION,
            DataSource.VERSION_KEY,
            THOUSAND_GENOMES_VERSION);
    String thousandGenomesUfId =
        WorkspaceUtils.createWorkspace(
                workspaceCreator, THOUSAND_GENOMES_WORKSPACE_NAME, /*description=*/ "", properties)
            .id;
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + THOUSAND_GENOMES_BUCKET_RESOURCE_NAME,
        "--bucket-name=" + THOUSAND_GENOMES_BUCKET_NAME,
        "--workspace=" + thousandGenomesUfId);
    TestCommand.runAndParseCommandExpectSuccess(
        UFGitRepo.class,
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + THOUSAND_GENOMES_GIT_RESOURCE_NAME,
        "--repo-url=" + GIT_REPO_SSH_URL);

    // Add 1000 Genomes data source to researcher workspace. We don't support add-ref for data
    // sources (see PF-1742), so call WSM directly.
    UUID researcherWorkspaceUuid =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(getUserFacingId()).getId();
    thousandGenomesUuid =
        WorkspaceManagerService.fromContext()
            .getWorkspaceByUserFacingId(thousandGenomesUfId)
            .getId();
    WorkspaceManagerService.fromContext()
        .createReferencedTerraWorkspace(
            researcherWorkspaceUuid, thousandGenomesUuid, THOUSAND_GENOMES_RESOURCE_NAME);
    System.out.println("Created data source resource with name " + THOUSAND_GENOMES_RESOURCE_NAME);
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
            .filter(resource -> resource.name.equals(THOUSAND_GENOMES_RESOURCE_NAME))
            .collect(MoreCollectors.onlyElement());
    assertThousandGenomesResource(actual);

    // `terra resource describe --name=$name`
    actual =
        TestCommand.runAndParseCommandExpectSuccess(
            UFDataSource.class, "resource", "describe", "--name=" + THOUSAND_GENOMES_RESOURCE_NAME);

    // Assert resource describe result
    assertThousandGenomesResource(actual);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + THOUSAND_GENOMES_RESOURCE_NAME, "--quiet");

    // Assert resource was deleted
    actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_SOURCE");
    actualResources.stream()
        .noneMatch(resource -> resource.name.equals(THOUSAND_GENOMES_RESOURCE_NAME));
  }

  @Test
  public void resolve() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resolve --name=$THOUSAND_GENOMES_RESOURCE_NAME`
    JSONObject resolve =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + THOUSAND_GENOMES_RESOURCE_NAME);
    assertThousandGenomesResourceResolve(resolve);

    // `terra resolve --name=$THOUSAND_GENOMES_RESOURCE_NAME/`
    JSONObject resolve2 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve", "--name=" + THOUSAND_GENOMES_RESOURCE_NAME + "/");
    assertThousandGenomesResourceResolve(resolve2);

    // `terra resolve --name=$THOUSAND_GENOMES_RESOURCE_NAME/$THOUSAND_GENOMES_BUCKET_RESOURCE_NAME`
    JSONObject resolve3 =
        TestCommand.runAndGetJsonObjectExpectSuccess(
            "resolve",
            "--name="
                + THOUSAND_GENOMES_RESOURCE_NAME
                + "/"
                + THOUSAND_GENOMES_BUCKET_RESOURCE_NAME);
    assertEquals(
        ExternalGCSBuckets.getGsPath(THOUSAND_GENOMES_BUCKET_NAME),
        resolve3.get(THOUSAND_GENOMES_BUCKET_RESOURCE_NAME));
    assertEquals(1, resolve3.length());

    // `terra resolve
    // --name=$THOUSAND_GENOMES_RESOURCE_NAME/$THOUSAND_GENOMES_BUCKET_RESOURCE_NAME/random`
    var err =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name="
                + THOUSAND_GENOMES_RESOURCE_NAME
                + "/"
                + THOUSAND_GENOMES_BUCKET_RESOURCE_NAME
                + "/"
                + RandomStringUtils.random(10));
    assertTrue(err.contains("Invalid path"));

    // `terra resolve --name=$THOUSAND_GENOMES_RESOURCE_NAME/<random>`
    var err2 =
        TestCommand.runCommandExpectExitCode(
            1,
            "resolve",
            "--name=" + THOUSAND_GENOMES_RESOURCE_NAME + "/" + RandomStringUtils.random(10));
    assertTrue(err2.contains("Invalid path"));
  }

  private void assertThousandGenomesResourceResolve(JSONObject resolve) {
    assertEquals(
        ExternalGCSBuckets.getGsPath(THOUSAND_GENOMES_BUCKET_NAME),
        resolve.get(THOUSAND_GENOMES_BUCKET_RESOURCE_NAME));
    assertEquals(GIT_REPO_SSH_URL, resolve.get(THOUSAND_GENOMES_GIT_RESOURCE_NAME));
    assertEquals(2, resolve.length());
  }

  private void assertThousandGenomesResource(UFDataSource actual) {
    // For some reason resourceType is null, so don't assert resourceType.
    assertEquals(thousandGenomesUuid, actual.dataSourceWorkspaceUuid);
    assertEquals(THOUSAND_GENOMES_WORKSPACE_NAME, actual.title);
    assertEquals(THOUSAND_GENOMES_SHORT_DESCRIPTION, actual.shortDescription);
    assertEquals(THOUSAND_GENOMES_VERSION, actual.version);
  }
}
