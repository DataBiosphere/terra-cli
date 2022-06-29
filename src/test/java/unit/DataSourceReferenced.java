package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
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
  private static final String thousandGenomesDataSourceWorkspaceName = "1000 Genomes";
  private static final String thousandGenomesResourceName =
      TestUtils.appendRandomNumber("1000-genomes-ref");
  private UUID thousandGenomesUuid;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();

    // Set up 1000 Genomes data source workspace
    UFWorkspace thousandGenomesWorkspace =
        WorkspaceUtils.createWorkspace(
            workspaceCreator, thousandGenomesDataSourceWorkspaceName, /*description=*/ "");

    // Add 1000 Genomes data source to researcher workspace. This isn't supported by CLI, so call
    // WSM directly.
    UUID researcherWorkspaceUuid =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(getUserFacingId()).getId();
    thousandGenomesUuid =
        WorkspaceManagerService.fromContext()
            .getWorkspaceByUserFacingId(thousandGenomesWorkspace.id)
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
    UFDataSource actualResource =
        actualResources.stream()
            .filter(resource -> resource.name.equals(thousandGenomesResourceName))
            .collect(MoreCollectors.onlyElement());
    // For some reason resourceType is null, so don't assert resourceType.
    assertEquals(thousandGenomesUuid, actualResource.dataSourceWorkspaceUuid);
    assertEquals(thousandGenomesDataSourceWorkspaceName, actualResource.title);

    // `terra resource describe --name=$name`
    actualResource =
        TestCommand.runAndParseCommandExpectSuccess(
            UFDataSource.class, "resource", "describe", "--name=" + thousandGenomesResourceName);

    // Assert resource describe result
    assertEquals(thousandGenomesUuid, actualResource.dataSourceWorkspaceUuid);
    assertEquals(thousandGenomesDataSourceWorkspaceName, actualResource.title);

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
}
