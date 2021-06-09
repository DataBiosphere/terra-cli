package unit;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static harness.utils.ExternalBQDatasets.randomDatasetId;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertTrue(
        cmd.stdOut.contains(Context.getPetSaKeyFile().getFileName().toString()),
        "GOOGLE_APPLICATION_CREDENTIALS set to pet SA key file");

    // `terra app execute echo \$GOOGLE_APPLICATION_CREDENTIALS`
    cmd = TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");

    // check that GOOGLE_CLOUD_PROJECT = workspace project
    assertTrue(
        cmd.stdOut.contains(workspace.googleProjectId),
        "GOOGLE_CLOUD_PROJECT set to workspace project");
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
    assertTrue(
        cmd.stdOut.contains("gs://" + bucketName),
        "TERRA_$resourceName set to resolved bucket path");

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
    assertTrue(
        cmd.stdOut.contains(workspace.googleProjectId), "gcloud project = workspace project");

    // `terra gcloud config get-value account`
    cmd = TestCommand.runCommand("gcloud", "config", "get-value", "account");
    assertTrue(
        cmd.stdOut.contains(Context.requireUser().getPetSACredentials().getClientEmail()),
        "gcloud account = pet SA email");
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
        cmd.stdOut.contains("0            gs://" + bucketName), "gsutil says bucket size = 0");

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
    assertTrue(
        cmd.stdOut.contains("\"id\": \"" + dataset.projectId + ":" + dataset.datasetId + "\""),
        "bq show includes the dataset id");

    // `terra resources delete --name=$name`
    TestCommand.runCommandExpectSuccess("resources", "delete", "--name=" + name);
  }

  @Test
  @DisplayName("check nextflow version")
  void nextflowVersion() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra nextflow run hello`
    TestCommand.Result cmd = TestCommand.runCommand("nextflow", "-version");
    assertTrue(cmd.stdOut.contains("http://nextflow.io"), "nextflow version ran successfully");
  }

  @Test
  @DisplayName("exit code is passed through to CLI caller")
  void exitCodePassedThrough() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getWorkspaceId());

    // `terra gcloud -version`
    // this is another malformed command, should be --version
    TestCommand.runCommandExpectExitCode(2, "gcloud", "-version");

    // `terra gcloud --version`
    // this is the correct version of the command
    TestCommand.Result cmd = TestCommand.runCommand("gcloud", "--version");
    assertTrue(cmd.stdOut.contains("Google Cloud SDK"), "gcloud version ran successfully");

    // `terra app execute exit 123`
    // this just returns an arbitrary exit code (similar to doing (exit 123); echo "$?" in a
    // terminal)
    TestCommand.runCommandExpectExitCode(123, "app", "execute", "exit", "123");
  }
}
