package unit;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
    assertTrue(appList.containsAll(Arrays.asList("gcloud", "gsutil", "bq", "nextflow")));
  }

  @Test
  @DisplayName("env vars include workspace cloud project and pet SA key file")
  @SuppressFBWarnings(
      value = "NP_NULL_ON_SOME_PATH",
      justification =
          "Pet SA key file in the Context must be populated if the earlier login() call succeeded.")
  void workspaceEnvVars() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId());

    // `terra app execute echo \$GOOGLE_APPLICATION_CREDENTIALS`
    TestCommand.Result cmd =
        TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_APPLICATION_CREDENTIALS");

    // check that GOOGLE_APPLICATION_CREDENTIALS = path to pet SA key file
    assertThat(
        "GOOGLE_APPLICATION_CREDENTIALS set to pet SA key file",
        cmd.stdOut,
        CoreMatchers.containsString(Context.getPetSaKeyFile().getFileName().toString()));

    // `terra app execute echo \$GOOGLE_APPLICATION_CREDENTIALS`
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
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceEnvVars";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra app execute echo \$TERRA_$name`
    TestCommand.Result cmd = TestCommand.runCommand("app", "execute", "echo", "$TERRA_" + name);

    // check that TERRA_$name = resolved bucket name
    assertThat(
        "TERRA_$resourceName set to resolved bucket path",
        cmd.stdOut,
        CoreMatchers.containsString("gs://" + bucketName));

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("gcloud is configured with the workspace project and pet SA key")
  void gcloudConfigured() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    UFWorkspace workspace =
        TestCommand.runAndParseCommandExpectSuccess(
            UFWorkspace.class, "workspace", "set", "--id=" + getWorkspaceId());

    // `terra gcloud config get-value project`
    TestCommand.Result cmd = TestCommand.runCommand("gcloud", "config", "get-value", "project");
    assertThat(
        "gcloud project = workspace project",
        cmd.stdOut,
        CoreMatchers.containsString(workspace.googleProjectId));

    // `terra gcloud config get-value account`
    cmd = TestCommand.runCommand("gcloud", "config", "get-value", "account");
    assertThat(
        "gcloud account = pet SA email",
        cmd.stdOut,
        CoreMatchers.containsString(Context.requireUser().getPetSACredentials().getClientEmail()));
  }

  @Test
  @DisplayName("gsutil bucket size = 0")
  void gsutilBucketSize() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "gsutilBucketSize";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resources", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `terra gsutil du -s \$TERRA_$name`
    TestCommand.Result cmd = TestCommand.runCommand("gsutil", "du", "-s", "$TERRA_" + name);
    assertTrue(
        cmd.stdOut.matches("(?s).*0\\s+gs://" + bucketName + ".*"), "gsutil says bucket size = 0");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("bq show dataset metadata")
  void bqShow() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra resources create bq-dataset --name=$name --dataset-id=$datasetId --format=json`
    String name = "bqShow";
    String datasetId = randomDatasetId();
    UFBqDataset dataset =
        TestCommand.runAndParseCommandExpectSuccess(
            UFBqDataset.class,
            "resources",
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
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
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
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // `terra app execute exit 123`
    // this just returns an arbitrary exit code (similar to doing (exit 123); echo "$?" in a
    // terminal)
    TestCommand.Result cmd = TestCommand.runCommand("app", "execute", "exit", "123");

    // check that the exit code is either 123 from the `exit 123` command, or 127 because gcloud is
    // not installed on this machine.
    // this is running in a local process, not a docker container so we don't have control over
    // what's installed.
    // both 123 and 127 indicate that the CLI is not swallowing error codes.
    assertTrue(
        cmd.exitCode == 123 || cmd.exitCode == 127,
        "app execute via local process returned an error code");
  }
}
