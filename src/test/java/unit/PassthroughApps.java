package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.TestCommand.Result;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the `terra app` commands and the pass-through apps: `terra gcloud`, `terra gsutil`,
 * `terra bq`, `terra nextflow`.
 *
 * <p>The test installer uses the Docker Available option, which sets DOCKER_CONTAINER as the
 * default app-launch. Note that the user installation path now defaults to LOCAL_PROCESS. In order
 * to change as few tests as possible and still get coverage of local mode apps, I've left the test
 * installer alone and simply set the app-launch mode to LOCAL_PROCESS for the appropriate tests.
 */
@Tag("unit")
public class PassthroughApps extends SingleWorkspaceUnit {
  @Test
  @DisplayName("app list returns all pass-through apps")
  void appList() throws IOException {
    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    // `terra app list --format=json`
    List<String> appList =
        TestCommand.runAndParseCommandExpectSuccess(new TypeReference<>() {}, "app", "list");

    // check that all pass-through apps are returned
    assertTrue(appList.containsAll(Arrays.asList("gcloud", "gsutil", "bq", "nextflow", "git")));
  }

  @Test
  @DisplayName("env vars include workspace cloud project")
  void workspaceEnvVars() throws IOException {
    workspaceCreator.login();
    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId());

    // `terra app execute echo \$GOOGLE_CLOUD_PROJECT`
    TestCommand.Result cmd =
        TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");

    // check that GOOGLE_CLOUD_PROJECT = workspace project
    assertThat(
        "GOOGLE_CLOUD_PROJECT set to workspace project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace.googleProjectId));
  }

  @Test
  @DisplayName("env vars include a resolved workspace resource - Docker container")
  void resourceEnvVars_dockerContainer() throws IOException {
    // `terra config set app-launch DOCKER_CONTAINER`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "DOCKER_CONTAINER");
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceEnvVarsDockerContainer";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra app execute echo \$TERRA_$name`
    // (We don't need the backslash when passing the command this way, as interpolation is delayed.
    Result cmd = TestCommand.runCommand("app", "execute", "echo", "$TERRA_" + name);

    // check that TERRA_$name = resolved bucket name
    assertThat(
        "TERRA_$resourceName set to resolved bucket path",
        cmd.stdOut,
        CoreMatchers.containsString(ExternalGCSBuckets.getGsPath(bucketName)));

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("env vars include a resolved workspace resource - local process")
  void resourceEnvVars_localProcess() throws IOException {
    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    // for some reason, running `terra app execute echo \$TERRA_<envVarName> doesn't work in
    // local-process mode. However, the env command correctly displays the Terra-created variables.
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceEnvVarsLocalProcess";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    Result cmd = TestCommand.runCommand("app", "execute", "env");
    assertThat(cmd.stdOut, containsString("TERRA_" + name + "=" + bucketName));
  }

  @Test
  @DisplayName("gcloud is configured in Docker with the workspace project and user")
  void gcloudConfigured_dockerContainer() throws IOException {
    // `terra config set app-launch DOCKER_CONTAINER`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "DOCKER_CONTAINER");
    gcloudConfiguredImpl();
  }

  @Test
  @DisplayName("gcloud is configured locally with the workspace project and user")
  void gcloudConfigured_localProcess() throws IOException {
    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");
    gcloudConfiguredImpl();
  }

  private void gcloudConfiguredImpl() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId());

    // `terra gcloud config get-value project`
    Result cmd = TestCommand.runCommand("gcloud", "config", "get-value", "project");
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
  @DisplayName("gsutil bucket size = 0")
  void gsutilBucketSize() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "gsutilBucketSize";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra gsutil du -s \$TERRA_$name`
    TestCommand.Result cmd = TestCommand.runCommand("gsutil", "du", "-s", "$TERRA_" + name);
    assertTrue(
        cmd.stdOut.matches("(?s).*0\\s+" + ExternalGCSBuckets.getGsPath(bucketName) + ".*"),
        "gsutil says bucket size = 0");

    // `terra resource delete --name=$name`
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + name, "--quiet");
  }

  @Test
  @DisplayName("bq show dataset metadata")
  void bqShow() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resource create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "bqShow";
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
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra nextflow -version`
    TestCommand.Result cmd = TestCommand.runCommand("nextflow", "-version");
    assertThat(
        "nextflow version ran successfully",
        cmd.stdOut,
        CoreMatchers.containsString("http://nextflow.io"));
  }

  @Test
  @DisplayName("running app not found on system gives warning")
  void runAppNotFound() throws IOException {
    workspaceCreator.login();
    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // Nextflow is not installed on the test machine itself, only in the Dockerfile.
    Result result = TestCommand.runCommand("nextflow", "run", "hello");
    assertTrue(result.stdOut.isEmpty(), "No output to std out");
    assertTrue(
        result.stdErr.contains("Please verify it is installed and included in the PATH."),
        "Error message is displayed.");
  }

  @Test
  @DisplayName("did you mean nextflow?")
  void suggestNameForMisspelling() throws IOException {
    // `terra nextphlow run hello`
    Result result = TestCommand.runCommand("nextphlow", "run", "hello");
    // Sometimes it says "Did you mean nextflow?" and sometimes it says "Did you mean: nextflow or
    // gcloud?"
    assertThat(
        "Error message asks question", result.stdErr, CoreMatchers.containsString("Did you mean"));
    assertThat(
        "Error message contains correct spelling",
        result.stdErr,
        CoreMatchers.containsString("nextflow"));

    assertThat("No output to stdout", result.stdOut, CoreMatchers.is(emptyString()));
  }

  @Test
  @DisplayName("git clone --all")
  void gitCloneAll() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=repo1",
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=repo2",
        "--repo-url=https://github.com/DataBiosphere/terra.git");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=repo3",
        "--repo-url=https://github.com/DataBiosphere/terra.git");

    // `terra git clone --all`
    TestCommand.runCommandExpectSuccess("git", "clone", "--all");

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    assertTrue(Files.exists(Paths.get(System.getProperty("user.dir"), "terra", ".git")));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra"));
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=repo1", "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=repo2", "--quiet");
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=repo3", "--quiet");
  }

  @Test
  @DisplayName("git clone resource")
  void gitCloneResource_dockerContainer() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=repo1",
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");

    // `terra git clone --resource=repo2`
    TestCommand.runCommandExpectSuccess("git", "clone", "--resource=repo1");

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=repo1", "--quiet");
  }

  @Test
  @DisplayName("git clone resource")
  void gitCloneResource_localProcess() throws IOException {
    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=repo1",
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");

    // `terra git clone --resource=repo2`
    TestCommand.runCommandExpectSuccess("git", "clone", "--resource=repo1");

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=repo1", "--quiet");
  }

  @Test
  @DisplayName("exit code is passed through to CLI caller in docker container")
  void exitCodePassedThroughDockerContainer() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra gcloud -version`
    // this is a malformed command, should be --version
    TestCommand.runCommandExpectExitCode(2, "gcloud", "-version");

    // `terra gcloud --version`
    // this is the correct version of the command
    Result cmd = TestCommand.runCommand("gcloud", "--version");
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
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

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
}
