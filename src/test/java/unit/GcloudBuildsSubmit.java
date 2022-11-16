package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra gcloud builds submit` commands. */
@Tag("unit")
public class GcloudBuildsSubmit extends SingleWorkspaceUnit {

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    new File("./Dockerfile").createNewFile();
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();

    // delete the dockerfile
    new File("./Dockerfile").delete();
  }

  @Test
  @DisplayName("builds submit --gcs-bucket")
  void build() throws IOException {
    Server server = Context.getServer();
    if (!server.getCloudBuildEnabled()) {
      return;
    }
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra resource create gcs-bucket --name=$name --format=json`
    String bucketResourceName = "resourceName";
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + bucketResourceName);

    // `builds submit --async --gcs-bucket-resource=bucketName --tag=$tag`
    TestCommand.runCommandExpectSuccess(
        "gcloud",
        "builds",
        "submit",
        // --async is required only for test environment without the access of creating repo
        "--async",
        "--gcs-bucket-resource=" + bucketResourceName,
        // gcloud builds submit requires --tag flag and follows the format in
        // https://cloud.google.com/sdk/gcloud/reference/builds/submit#--tag.
        "--tag=us-central1-docker.pkg.dev/$GOOGLE_CLOUD_PROJECT/image_`date +'%Y%m%d'`");

    // `terra resource delete --name=$name`
    // TODO:remove the timeout after PF-2205 done.
    TestCommand.Result cmd =
        TestCommand.runCommand("resource", "delete", "--name=" + bucketResourceName, "--quiet");

    boolean cliTimedOut =
        cmd.exitCode == 1
            && cmd.stdErr.contains(
                "CLI timed out waiting for the job to complete. It's still running on the server.");
    assertTrue(cmd.exitCode == 0 || cliTimedOut, "delete either succeeds or times out");
  }
}
