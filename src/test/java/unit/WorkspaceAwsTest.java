package unit;

import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_AWS_VAULT_PATH;
import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_CACHE_WITH_AWS_VAULT;
import static bio.terra.cli.utils.AwsConfiguration.DEFAULT_TERRA_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFStatus;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.cli.utils.AwsConfiguration;
import bio.terra.workspace.model.CloudPlatform;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import harness.utils.AwsConfigurationTestUtils;
import harness.utils.TestUtils;
import harness.utils.WorkspaceUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra workspace` commands specific to CloudPlatform.AWS. */
@Tag("unit-aws")
public class WorkspaceAwsTest extends ClearContextUnit {
  private static final String namePrefix = "cliTestAwsWorkspace";

  @BeforeAll
  protected void setupOnce() throws Exception {
    setCloudPlatform(CloudPlatform.AWS);
    super.setupOnce();
  }

  private static <T extends UFWorkspaceLight, E extends UFWorkspaceLight>
      void assertWorkspaceAwsFields(T expected, E actual, String messageSource) {
    assertEquals(expected.id, actual.id, "workspace id matches that in " + messageSource);
    assertEquals(
        expected.awsMajorVersion,
        actual.awsMajorVersion,
        "workspace aws major version matches that in " + messageSource);
    assertEquals(
        expected.awsOrganizationId,
        actual.awsOrganizationId,
        "workspace aws organization id matches that in " + messageSource);
    assertEquals(
        expected.awsAccountId,
        actual.awsAccountId,
        "workspace aws account id matches that in " + messageSource);
    assertEquals(
        expected.awsTenantAlias,
        actual.awsTenantAlias,
        "workspace aws tenant alias matches that in " + messageSource);
    assertEquals(
        expected.awsEnvironmentAlias,
        actual.awsEnvironmentAlias,
        "workspace aws environment alias matches that in " + messageSource);
  }

  @Test
  @DisplayName("status and describe list reflect workspace create")
  void statusDescribeListReflectCreate() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));

    // check the created workspace has an id and aws details
    assertNotNull(createdWorkspace.id, "create workspace returned a workspace id");
    assertNotNull(
        createdWorkspace.awsMajorVersion, "create workspace returned a aws major version");
    assertNotNull(
        createdWorkspace.awsOrganizationId, "create workspace returned a aws organization id");
    assertNotNull(createdWorkspace.awsAccountId, "create workspace returned a aws account id");
    assertNotNull(createdWorkspace.awsTenantAlias, "create workspace returned a aws tenant alias");
    assertNotNull(
        createdWorkspace.awsEnvironmentAlias, "create workspace returned a aws environment alias");
    assertThat(
        "workspace email matches test user",
        createdWorkspace.userEmail,
        equalToIgnoringCase(testUser.email));
    assertThat(
        "workspace cloudPlatform matches AWS",
        CloudPlatform.AWS,
        equalTo(createdWorkspace.cloudPlatform));

    // `terra status --format=json`
    UFStatus status = TestCommand.runAndParseCommandExpectSuccess(UFStatus.class, "status");

    // check the current status reflects the new workspace
    assertThat(
        "workspace server matches current server",
        createdWorkspace.serverName,
        equalToIgnoringCase(status.server.name));
    assertWorkspaceAwsFields(createdWorkspace, status.workspace, "current status");

    // `terra workspace describe --format=json`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");

    // check the new workspace is returned by describe
    assertWorkspaceAwsFields(createdWorkspace, describedWorkspace, "describe");

    // check the new workspace is included in the list
    List<UFWorkspaceLight> matchingWorkspaces =
        WorkspaceUtils.listWorkspacesWithId(createdWorkspace.id);
    assertEquals(1, matchingWorkspaces.size(), "new workspace is included exactly once in list");
    assertWorkspaceAwsFields(createdWorkspace, matchingWorkspaces.get(0), "list");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("AWS workspace describe reflects the number of resources")
  void describeReflectsNumResources() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));
    assertEquals(0, createdWorkspace.numResources, "new workspace has 0 resources");

    // `terra resource create s3-storage-folder --name=$name`
    String storageName = TestUtils.appendRandomString(namePrefix);
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "s3-storage-folder", "--name=" + storageName);

    // `terra workspace describe`
    UFWorkspace describedWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(UFWorkspace.class, "workspace", "describe");
    assertEquals(
        1,
        describedWorkspace.numResources,
        "workspace has 1 resource after creating storage folder");

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("AWS configure command")
  void configureAws() throws IOException, InterruptedException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();

    UFWorkspace createdWorkspace =
        WorkspaceUtils.createWorkspace(testUser, Optional.of(getCloudPlatform()));
    assertEquals(0, createdWorkspace.numResources, "new workspace has 0 resources");

    // verify config file created with defaults
    Path configFilePath = AwsConfiguration.getConfigFilePath(createdWorkspace.uuid);
    assertTrue(configFilePath.toFile().exists(), "AWS configuration file created");

    AwsConfiguration awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertEquals(
        configFilePath.getFileName().toString(),
        awsConfiguration.getFilePath().getFileName().toString(),
        "file name in configuration matches expected");
    assertEquals(
        DEFAULT_TERRA_PATH,
        awsConfiguration.getTerraPath(),
        "terra path in configuration matches default");
    assertEquals(
        DEFAULT_AWS_VAULT_PATH,
        awsConfiguration.getAwsVaultPath(),
        "aws vault path in configuration matches default");
    assertEquals(
        DEFAULT_CACHE_WITH_AWS_VAULT,
        awsConfiguration.getCacheWithAwsVault(),
        "cache with aws vault path in configuration matches default");
    assertFalse(
        awsConfiguration.getDefaultResourceName().isPresent(),
        "default resource name not set in configuration");
    assertEquals(0, awsConfiguration.getResourceCount(), "configuration has no resources");

    // 'terra workspace configure-aws' - should not error with no resources
    TestCommand.Result configureResult =
        TestCommand.runCommandExpectSuccess("workspace", "configure-aws");

    Path configOutputPath =
        AwsConfigurationTestUtils.getProfilePathFromOutput(configureResult.stdOut);
    assertEquals(
        configFilePath.getFileName().toString(),
        configOutputPath.getFileName().toString(),
        "file name in configure-aws output matches expected");

    String folder1 = TestUtils.appendRandomString(namePrefix);
    String awsRegion = "us-east-1";
    Collection<String> resourceNames = Set.of(folder1);

    // `terra resource create s3-storage-folder --name=$name --region=$region` - resource added
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "s3-storage-folder", "--name=" + folder1, "--region=" + awsRegion);
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertTrue(
        awsConfiguration.getDefaultResourceName().isEmpty(), "default resource name not set");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    // 'terra workspace configure-aws --default-resource=$name'
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "configure-aws",
        "--default-resource=" + folder1,
        "--cache-with-aws-vault=true");
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertTrue(awsConfiguration.getCacheWithAwsVault(), "cache with aws vault is true");
    assertEquals(
        folder1, awsConfiguration.getDefaultResourceName().get(), "default resource name is set");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    String folder2 = TestUtils.appendRandomString(namePrefix);
    resourceNames = Set.of(folder1, folder2);

    // `terra resource create s3-storage-folder --name=$name --region=$region` - resource added
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "s3-storage-folder", "--name=" + folder2, "--region=" + awsRegion);
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertEquals(
        folder1,
        awsConfiguration.getDefaultResourceName().orElse(null),
        "default resource name unchanged");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    String terraPath = "/fake/path/to/terra";
    String awsVaultPath = "/fake/path/to/aws-vault";

    // 'terra workspace configure-aws --terra-path=$path --aws-vault-path=$path
    // --cache-with-aws-vault --default-resource=$name'
    TestCommand.runCommandExpectSuccess(
        "workspace",
        "configure-aws",
        "--terra-path=" + terraPath,
        "--aws-vault-path=" + awsVaultPath,
        "--cache-with-aws-vault",
        "--default-resource=" + folder1);
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertEquals(
        terraPath, awsConfiguration.getTerraPath(), "terra path in configuration matches expected");
    assertEquals(
        awsVaultPath,
        awsConfiguration.getAwsVaultPath(),
        "aws vault path in configuration matches expected");
    assertTrue(awsConfiguration.getCacheWithAwsVault(), "cache with aws vault is true");
    assertEquals(
        folder1,
        awsConfiguration.getDefaultResourceName().orElse(null),
        "default resource name unchanged");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    String anotherAwsVaultPath = "/another/fake/path/to/aws-vault";
    // 'terra workspace configure-aws --aws-vault-path=$path --append'
    TestCommand.runCommandExpectSuccess(
        "workspace", "configure-aws", "--aws-vault-path=" + anotherAwsVaultPath, "--append");
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertEquals(
        terraPath, awsConfiguration.getTerraPath(), "terra path in configuration unchanged");
    assertEquals(
        anotherAwsVaultPath,
        awsConfiguration.getAwsVaultPath(),
        "aws vault path in configuration matches expected");
    assertTrue(awsConfiguration.getCacheWithAwsVault(), "cache with aws vault unchanged");
    assertEquals(
        folder1,
        awsConfiguration.getDefaultResourceName().orElse(null),
        "default resource name unchanged");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    // 'terra resource delete --name=$defaultResourceName --quiet' - default resource deleted, other
    // options are retained
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + folder1, "--quiet");
    resourceNames = Set.of(folder2);
    awsConfiguration = AwsConfiguration.loadFromDisk(createdWorkspace.uuid);
    assertEquals(
        terraPath, awsConfiguration.getTerraPath(), "terra path in configuration unchanged");
    assertEquals(
        anotherAwsVaultPath,
        awsConfiguration.getAwsVaultPath(),
        "aws vault path in configuration unchanged");
    assertTrue(
        awsConfiguration.getCacheWithAwsVault(), "cache with aws vault in configuration unchanged");
    assertTrue(
        awsConfiguration.getDefaultResourceName().isEmpty(), "default resource name is removed");
    AwsConfigurationTestUtils.validateConfiguration(awsConfiguration, awsRegion, resourceNames);

    // `terra workspace delete` - configuration file deleted
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
    assertFalse(configFilePath.toFile().exists(), "AWS configuration file deleted");
  }
}
