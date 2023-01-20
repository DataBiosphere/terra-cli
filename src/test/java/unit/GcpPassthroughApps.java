package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.gax.paging.Page;
import com.google.cloud.Identity;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import harness.TestCommand;
import harness.TestContext;
import harness.baseclasses.SingleWorkspaceUnitGcp;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import harness.utils.TestUtils;
import harness.utils.WorkspaceUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the `terra app` commands and the pass-through apps: `terra gcloud`, `terra gsutil`,
 * `terra bq`, `terra nextflow`.
 *
 * <p>Important: all tests using these commands should live inside this class! We often run tests in
 * parallel, and the test class is the unit of parallelization. These tools each maintain their own
 * global state in various places, and they will clobber eachother if they run in multiple test
 * runners at once.
 */
@Tag("unit-gcp")
public class GcpPassthroughApps extends SingleWorkspaceUnitGcp {
  // external bucket to use for testing the JSON format against GCS directly
  private BucketInfo externalBucket;

  @Override
  @BeforeAll
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalBucket = ExternalGCSBuckets.createBucketWithUniformAccess();

    // grant the user's proxy group write access to the bucket, so we can test calling `terra gsutil
    // lifecycle` with the same JSON format used for creating controlled bucket resources with
    // lifecycle rules
    ExternalGCSBuckets.grantWriteAccess(externalBucket, Identity.group(Auth.getProxyGroupEmail()));

    // TODO: stolen from base class
    TestContext.clearGcloudConfigDirectory();
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalGCSBuckets.deleteBucket(externalBucket);
    externalBucket = null;
  }

  @Test
  @DisplayName("app list returns all pass-through apps")
  void appList() throws IOException {
    // `terra app list --format=json`
    List<String> appList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "app", "list");

    // check that all pass-through apps are returned
    assertTrue(appList.containsAll(Arrays.asList("gcloud", "gsutil", "bq", "nextflow", "git")));
  }

  @Test
  @DisplayName("env vars include terra user and workspace cloud project")
  void workspaceEnvVars() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra app execute echo \$TERRA_USER_EMAIL`
    TestCommand.Result cmd = TestCommand.runCommand("app", "execute", "echo", "$TERRA_USER_EMAIL");

    // check that TERRA_USER_EMAIL = test user email"
    assertThat(
        "TERRA_USER_EMAIL set to test user email",
        cmd.stdOut,
        CoreMatchers.containsString(workspaceCreator.email.toLowerCase()));

    // `terra app execute echo \$GOOGLE_CLOUD_PROJECT`
    cmd = TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");

    // check that GOOGLE_CLOUD_PROJECT = workspace project
    assertThat(
        "GOOGLE_CLOUD_PROJECT set to workspace project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace.googleProjectId));
  }

  @Test
  @DisplayName("env vars include a resolved workspace resource")
  void resourceEnvVars() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    // Put dash in resource name. Dashes aren't allowed in env variables.
    String name = "resource-Env-Vars";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra app execute echo \$TERRA_$name`
    TestCommand.Result cmd =
        TestCommand.runCommand("app", "execute", "echo", "$TERRA_resource_Env_Vars");

    // check that TERRA_$name = resolved bucket name
    assertThat(
        "TERRA_$resourceName set to resolved bucket path",
        cmd.stdOut,
        CoreMatchers.containsString(ExternalGCSBuckets.getGsPath(bucketName)));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("gcloud is configured with the workspace project and user")
  void gcloudConfigured() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra gcloud config get-value project`
    TestCommand.Result cmd = TestCommand.runCommand("gcloud", "config", "get-value", "project");
    assertThat(
        "gcloud project = workspace project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace.googleProjectId));

    // `terra gcloud config get-value account`
    // Normally, when a human is involved, `gcloud auth login` or `gcloud auth
    // activate-service-account` writes `account` to properties file at
    // ~/.config/gcloud/configurations/config_default.
    //
    // However, there is no programmatic way to simulate this. `gcloud auth login` only supports
    // interactive mode. `gcloud auth activate-service-account` requires --key-file param. Even if
    // CLOUDSDK_AUTH_ACCESS_TOKEN is set, it wants --key-file param.
    //
    // When a human is involved, `account` in ~/.config/gcloud/configurations/config_default is
    // used. During unit tests, that is not used. Authentication is done through other means, such
    // as via CLOUDSDK_AUTH_ACCESS_TOKEN. So having test manually construct
    // ~/.config/gcloud/configurations/config_default and then assert its contents, is not useful.
    //
    // If `gcloud auth login` or `gcloud auth activate-service-account` can ever be done
    // programmatically (without key file), uncomment this test.
    //    cmd = TestCommand.runCommand("gcloud", "config", "get-value", "account");
    //    assertThat(
    //        "gcloud account = test user email",
    //        cmd.stdOut,
    //        CoreMatchers.containsString(Context.requireUser().getEmail()));
  }

  @Test
  @DisplayName("`gsutil ls` and `gcloud alpha storage ls`")
  void gsutilGcloudAlphaStorageLs() throws IOException, InterruptedException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    UFWorkspace createdWorkspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceName";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    Storage localProjectStorageClient =
        StorageOptions.newBuilder()
            .setProjectId(createdWorkspace.googleProjectId)
            .setCredentials(workspaceCreator.getCredentialsWithCloudPlatformScope())
            .build()
            .getService();

    // Poll until the test user can list GCS buckets in the workspace project, which may be delayed.
    Page<Bucket> createdBucketOnCloud =
        CrlUtils.callGcpWithPermissionExceptionRetries(localProjectStorageClient::list);

    // `terra gsutil ls`
    TestCommand.Result cmd = TestCommand.runCommand("gsutil", "ls");
    assertTrue(
        cmd.stdOut.contains(ExternalGCSBuckets.getGsPath(bucketName)),
        "`gsutil ls` returns bucket");

    // `terra gcloud alpha storage ls`
    cmd = TestCommand.runCommand("gcloud", "alpha", "storage", "ls");
    assertTrue(
        cmd.stdOut.contains(ExternalGCSBuckets.getGsPath(bucketName)),
        "`gcloud alpha storage ls` returns bucket");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("bq show dataset metadata")
  void bqShow() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = TestUtils.appendRandomNumber("bqShow");
    String datasetId = randomDatasetId();
    UFBqDataset dataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resource",
            "create",
            "bq-dataset",
            "--name=" + name,
            "--dataset-id=" + datasetId);

    // `terra bq show --format=prettyjson [project id]:[dataset id]`
    TestCommand.Result cmd = TestCommand.runCommand("bq", "show", "--format=prettyjson", datasetId);
    assertThat(
        "bq show includes the dataset id",
        cmd.stdOut,
        CoreMatchers.containsString(
            "\"id\": \"" + dataset.projectId + ":" + dataset.datasetId + "\""));

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("check nextflow version")
  void nextflowVersion() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra nextflow -version`
    TestCommand.Result cmd = TestCommand.runCommand("nextflow", "-version");
    assertThat(
        "nextflow version ran successfully",
        cmd.stdOut,
        CoreMatchers.containsString("http://nextflow.io"));
  }

  @Test
  @DisplayName("git clone --all")
  void gitCloneAll() throws IOException {
    String resource1Name = TestUtils.appendRandomNumber("repo1");

    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + resource1Name,
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");
    String resource2Name = createAGitRepoReferenceByCallingWsmEndpoint();

    // `terra git clone --all`
    TestCommand.runCommandExpectSuccess("git", "clone", "--all");

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"), "terra", ".git")));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra"));
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resource1Name, "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resource2Name, "--quiet");
  }

  @Test
  @DisplayName("git clone resource")
  void gitCloneResource() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    String repo1 = TestUtils.appendRandomNumber("repo");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + repo1,
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");
    String repo2 = TestUtils.appendRandomNumber("repo2");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + repo2,
        "--repo-url=https://github.com/DataBiosphere/terra.git");
    String bucketResourceName = "git_clone_bucket_resource_name";
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "gcs-bucket",
        "--name=" + bucketResourceName,
        "--bucket-name=" + TestUtils.appendRandomNumber("bucket-name"));

    // `terra git clone --resource=$repo1,repo2`
    TestCommand.runCommandExpectSuccess("git", "clone", "--resource=" + repo1 + "," + repo2);
    // `terra git clone --resource=$bucketResourceName`
    TestCommand.runCommandExpectExitCode(1, "git", "clone", "--resource=" + bucketResourceName);

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"), "terra", ".git")));

    // cleanup
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra"));
  }

  @Test
  @DisplayName("exit code is passed through to CLI caller in docker container")
  void exitCodePassedThroughDockerContainer() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra gcloud -version`
    // this is a malformed command, should be --version
    TestCommand.runCommandExpectExitCode(2, "gcloud", "-version");

    // `terra gcloud --version`
    // this is the correct version of the command
    TestCommand.Result cmd = TestCommand.runCommand("gcloud", "--version");
    assertThat(
        "gcloud version ran successfully",
        cmd.stdOut,
        CoreMatchers.containsString("Google Cloud SDK"));

    // `terra app execute exit 123`
    // this just returns an arbitrary exit code (similar to doing (exit 123); echo "$?" in a
    // terminal)
    TestCommand.runCommandExpectExitCode(123, "app", "execute", "exit", "123");
  }

  @Test
  @DisplayName("exit code is passed through to CLI caller in local process")
  void exitCodePassedThroughLocalProcess() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // `terra app execute exit 123`
    // this just returns an arbitrary exit code (similar to doing (exit 123); echo "$?" in a
    // terminal)
    TestCommand.Result cmd = TestCommand.runCommand("app", "execute", "exit", "123");

    // Check that the exit code is either 123 from the `exit 123` command, or 1 because gcloud
    // fails with `(gcloud.config.get-value) Failed to create the default configuration. Ensure your
    // have the correct permissions on`.
    // This is running in a local process, not a docker container so we don't have control over
    // what's installed.
    // Both 123 and 1 indicate that the CLI is not swallowing error codes.
    assertTrue(
        cmd.exitCode == 123 || cmd.exitCode == 1,
        "Expected to return exit code 123 or 1, instead got " + cmd.exitCode);
  }

  @Test
  @DisplayName("CLI uses the same format as gsutil for setting lifecycle rules")
  void sameFormatForExternalBucket() throws IOException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // the CLI mounts the current working directory to the Docker container when running apps
    // so we need to give it the path to lifecycle JSON file relative to the current working
    // directory. e.g.
    // lifecyclePathOnHost =
    // /Users/gh/terra-cli/src/test/resources/testinputs/gcslifecycle/multipleRules.json
    // currentDirOnHost = /Users/gh/terra-cli/
    // lifecyclePathOnContainer = ./src/test/resources/testinputs/gcslifecycle/multipleRules.json
    Path lifecyclePathOnHost = TestCommand.getPathForTestInput("gcslifecycle/multipleRules.json");
    Path currentDirOnHost = Path.of(System.getProperty("user.dir"));
    Path lifecyclePathOnContainer = currentDirOnHost.relativize(lifecyclePathOnHost);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    // `terra gsutil lifecycle set $lifecycle gs://$bucketname`
    TestCommand.runCommandExpectSuccess(
        "gsutil",
        "lifecycle",
        "set",
        lifecyclePathOnContainer.toString(),
        ExternalGCSBuckets.getGsPath(externalBucket.getName()));

    List<? extends BucketInfo.LifecycleRule> lifecycleRules =
        ExternalGCSBuckets.getLifecycleRulesFromCloud(externalBucket.getName(), workspaceCreator);
    GcsBucketLifecycle.validateMultipleRules(lifecycleRules);
  }

  @Test
  @DisplayName("gcloud and app execute respect workspace override")
  void gcloudAppExecute() throws IOException, InterruptedException {
    workspaceCreator.login(/*writeGcloudAuthFiles=*/ true);

    UFWorkspace workspace2 =
        WorkspaceUtils.createWorkspace(workspaceCreator, Optional.of(getCloudPlatform()));

    // Set workspace back to the original
    // `terra workspace set --id=$id1`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra app execute --workspace=$id2 echo \$GOOGLE_CLOUD_PROJECT`
    TestCommand.Result cmd =
        TestCommand.runCommand(
            "app", "execute", "--workspace=" + workspace2.id, "echo", "$GOOGLE_CLOUD_PROJECT");

    // check that the google cloud project id matches workspace 2
    assertThat(
        "GOOGLE_CLOUD_PROJECT set to workspace2's project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace2.googleProjectId));

    // `terra gcloud --workspace=$id2 config get project`
    cmd =
        TestCommand.runCommand(
            "gcloud", "--workspace=" + workspace2.id, "config", "get-value", "project");

    // check that the google cloud project id matches workspace 2
    assertThat(
        "gcloud project = workspace2's project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace2.googleProjectId));

    String gitResourceName = createAGitRepoReferenceByCallingWsmEndpoint();
    TestCommand.Result gitCommand = TestCommand.runCommand("app", "execute", "env");

    assertThat(
        gitCommand.stdOut,
        CoreMatchers.containsString(
            "TERRA_"
                + gitResourceName.replace("-", "_")
                + "="
                + "https://github.com/DataBiosphere/terra.git"));
  }

  @Test
  @DisplayName("cromwell generate-config with default dir")
  void cromwellGenerateConfig() throws IOException {
    workspaceCreator.login(true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config`
    TestCommand.runCommandExpectSuccess("cromwell", "generate-config");

    // New cromwell.conf file generate successfully.
    assertTrue(new File("cromwell.conf").isFile());

    // Remove the created config file.
    new File("cromwell.conf").delete();
  }

  @Test
  @DisplayName("cromwell generate-config with custom dir")
  void cromwellGenerateConfigCustomDir() throws IOException {
    workspaceCreator.login(true);

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config --path=build/cromwell.conf`
    TestCommand.runCommandExpectSuccess("cromwell", "generate-config", "--dir=build");

    // New cromwell.conf file generate successfully.
    assertTrue(
        new File("./build/cromwell.conf").isFile(), "New cromwell.conf file generate successfully");

    // Remove the created config file.
    new File("./build/cromwell.conf").delete();
  }

  /** This is to re-create a scenario when a resource is created through UI. */
  private String createAGitRepoReferenceByCallingWsmEndpoint() {
    String gitResourceName = TestUtils.appendRandomNumber("git-referenced-url");
    UUID workspaceUuid =
        WorkspaceManagerService.fromContext().getWorkspaceByUserFacingId(getUserFacingId()).getId();
    WorkspaceManagerService.fromContext()
        .createReferencedGitRepo(
            workspaceUuid,
            new AddGitRepoParams.Builder()
                .gitRepoUrl("https://github.com/DataBiosphere/terra.git")
                .resourceFields(
                    new CreateResourceParams.Builder()
                        .name(gitResourceName)
                        .description("This is a referenced resource created through UI")
                        .cloningInstructions(CloningInstructionsEnum.REFERENCE)
                        .stewardshipType(StewardshipType.REFERENCED)
                        .build())
                .build());
    return gitResourceName;
  }
}
