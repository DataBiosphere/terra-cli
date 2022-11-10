package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import harness.TestCommand;
import harness.TestUser;
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
  private TestUser shareUser;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();

    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    shareUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareUser.email, "--role=WRITER");
    shareUser.login();

    // create a dockerfile as building source
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

    // `builds submit --async --gcs-bucket=bucketName --tag`
    TestCommand.runCommandExpectSuccess(
        "gcloud",
        "builds",
        "submit",
        "--async",
        "--gcs-bucket=" + bucketResourceName,
        "--tag=us-central1-docker.pkg.dev/$GOOGLE_CLOUD_PROJECT/ml4h/papermill:`date +'%Y%m%d'`");

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
