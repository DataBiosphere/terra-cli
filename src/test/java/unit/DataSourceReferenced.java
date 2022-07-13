package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
import bio.terra.cli.serialization.userfacing.resource.UFGcsBucket;
import bio.terra.cli.service.WorkspaceManagerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.MoreCollectors;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.TestUtils;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra resource` commands that handle data sources. */
@Tag("unit")
public class DataSourceReferenced extends SingleWorkspaceUnit {
  private static final String thousandGenomesWorkspaceName = "1000 Genomes";
  private static final String thousandGenomesShortDescription = "short description";
  private static final String thousandGenomesVersion = "version";
  private static final String thousandGenomesBucketName = "bucket-name";
  private static final String thousandGenomesBucketResourceName =
      TestUtils.appendRandomNumber("bucket_resource_name");

  private static final String thousandGenomesResourceName =
      TestUtils.appendRandomNumber("1000-genomes-ref");

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
            thousandGenomesShortDescription,
            DataSource.VERSION_KEY,
            thousandGenomesVersion);
    String thousandGenomesUfId =
        WorkspaceUtils.createWorkspace(
                workspaceCreator, thousandGenomesWorkspaceName, /*description=*/ "", properties)
            .id;
    TestCommand.runAndParseCommandExpectSuccess(
        UFGcsBucket.class,
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + thousandGenomesBucketResourceName,
        "--bucket-name=" + thousandGenomesBucketName,
        "--workspace=" + thousandGenomesUfId);

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
            researcherWorkspaceUuid, thousandGenomesUuid, thousandGenomesResourceName);
    System.out.println("Created data source resource with name " + thousandGenomesResourceName);
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
            .filter(resource -> resource.name.equals(thousandGenomesResourceName))
            .collect(MoreCollectors.onlyElement());
    assertThousandGenomesResource(actual);

    // `terra resource describe --name=$name`
    actual =
        TestCommand.runAndParseCommandExpectSuccess(
            UFDataSource.class, "resource", "describe", "--name=" + thousandGenomesResourceName);

    // Assert resource describe result
    assertThousandGenomesResource(actual);

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess(
        "resource", "delete", "--name=" + thousandGenomesResourceName, "--quiet");

    // Assert resource was deleted
    actualResources =
        TestCommand.runAndParseCommandExpectSuccess(
            new TypeReference<>() {}, "resource", "list", "--type=DATA_SOURCE");
    actualResources.stream()
        .noneMatch(resource -> resource.name.equals(thousandGenomesResourceName));
  }

  private void assertThousandGenomesResource(UFDataSource actual) {
    // For some reason resourceType is null, so don't assert resourceType.
    assertEquals(thousandGenomesUuid, actual.dataSourceWorkspaceUuid);
    assertEquals(thousandGenomesWorkspaceName, actual.title);
    assertEquals(thousandGenomesShortDescription, actual.shortDescription);
    assertEquals(thousandGenomesVersion, actual.version);

    UFGcsBucket actualBucket = (UFGcsBucket) actual.resources.get(0);
    assertEquals(thousandGenomesBucketResourceName, actualBucket.name);
    assertEquals(thousandGenomesBucketName, actualBucket.bucketName);
  }
}
