package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.ExternalGCSBuckets;
import harness.utils.TestUtils;
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
 */
@Tag("unit")
public class PassthroughApps extends SingleWorkspaceUnit {
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
  @DisplayName("env vars include workspace cloud project")
  void workspaceEnvVars() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getUserFacingId());

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
  @DisplayName("env vars include a resolved workspace resource")
  void resourceEnvVars() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceEnvVars";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra app execute echo \$TERRA_$name`
    TestCommand.Result cmd = TestCommand.runCommand("app", "execute", "echo", "$TERRA_" + name);

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
    workspaceCreator.login();

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
  void gsutilGcloudAlphaStorageLs() throws IOException {
    // Use LOCAL_PROCESS because with DOCKER_CONTAINER and Test Distribution, we're unable to mount
    // ~/.config/gcloud (needed for authentication) into container started by terra cli.
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceName";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

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
    // Use LOCAL_PROCESS because with DOCKER_CONTAINER and Test Distribution, we're unable to mount
    // ~/.config/gcloud (needed for authentication) into container started by terra cli.
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    workspaceCreator.login();

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
    workspaceCreator.login();

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
    // Use LOCAL_PROCESS because with DOCKER_CONTAINER and Test Distribution, docker-in-docker
    // mounting doesn't work. After "git clone", the repos are cloned inside the container started
    // by terra cli. However, in the "host" (Test Distribution agent container), we're not able to
    // see the repos.
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    String resource1Name = TestUtils.appendRandomNumber("repo1");
    String resource2Name = TestUtils.appendRandomNumber("repo1");

    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + resource1Name,
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + resource2Name,
        "--repo-url=https://github.com/DataBiosphere/terra.git");

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
    // Use LOCAL_PROCESS because with DOCKER_CONTAINER and Test Distribution, docker-in-docker
    // mounting doesn't work. After "git clone", the repo is cloned inside the container started
    // by terra cli. However, in the "host" (Test Distribution agent container), we're not able to
    // see the repo.
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    String resourceName = TestUtils.appendRandomNumber("repo");

    workspaceCreator.login();
    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    TestCommand.runCommandExpectSuccess(
        "resource",
        "add-ref",
        "git-repo",
        "--name=" + resourceName,
        "--repo-url=https://github.com/DataBiosphere/terra-example-notebooks.git");

    // `terra git clone --resource=repo1`
    TestCommand.runCommandExpectSuccess("git", "clone", "--resource=" + resourceName);

    assertTrue(
        Files.exists(Paths.get(System.getProperty("user.dir"), "terra-example-notebooks", ".git")));
    FileUtils.deleteQuietly(new File(System.getProperty("user.dir") + "/terra-example-notebooks"));
    TestCommand.runCommandExpectSuccess("resource", "delete", "--name=" + resourceName, "--quiet");
  }

  @Test
  @DisplayName("exit code is passed through to CLI caller in docker container")
  void exitCodePassedThroughDockerContainer() throws IOException {
    workspaceCreator.login();

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
    workspaceCreator.login();

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
}
