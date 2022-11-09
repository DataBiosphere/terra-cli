package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import com.google.cloud.Identity;
import com.google.cloud.storage.BucketInfo;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.Auth;
import harness.utils.ExternalGCSBuckets;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra gcloud builds submit` commands. */
@Tag("unit")
public class GcloudBuildsSubmit extends SingleWorkspaceUnit {

  // external bucket to use for creating GCS bucket references in the workspace
  private BucketInfo externalSharedBucket;
  private TestUser shareUser;

  @BeforeAll
  @Override
  protected void setupOnce() throws Exception {
    super.setupOnce();
    externalSharedBucket = ExternalGCSBuckets.createBucketWithUniformAccess();

    // grant the user's proxy group access to the bucket so that it will pass WSM's access check
    // when adding it as a referenced resource
    ExternalGCSBuckets.grantWriteAccess(
        externalSharedBucket, Identity.group(Auth.getProxyGroupEmail()));

    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());
    shareUser = TestUser.chooseTestUserWhoIsNot(workspaceCreator);
    TestCommand.runCommandExpectSuccess(
        "workspace", "add-user", "--email=" + shareUser.email, "--role=WRITER");
    shareUser.login();
    ExternalGCSBuckets.grantReadAccess(
        externalSharedBucket, Identity.group(Auth.getProxyGroupEmail()));

    // create a dockerfile
    new File("./Dockerfile").createNewFile();
  }

  @AfterAll
  @Override
  protected void cleanupOnce() throws Exception {
    super.cleanupOnce();
    ExternalGCSBuckets.deleteBucket(externalSharedBucket);
    externalSharedBucket = null;

    // create dockerfile as building source
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

    // `terra resource create gcs-bucket --name=$name --bucket-name=$bucketName --format=json`
    String name = "resourceName";
    String bucketName = UUID.randomUUID().toString();
    TestCommand.runCommandExpectSuccess(
        "resource", "create", "gcs-bucket", "--name=" + name, "--bucket-name=" + bucketName);

    // `builds submit --gcs-bucket=bucketName`
    TestCommand.runCommandExpectSuccess("gcloud", "builds", "submit", "--gcs-bucket=" + name);

    // `terra resource delete --name=$name`
    TestCommand.Result cmd =
        TestCommand.runCommand("resource", "delete", "--name=" + name, "--quiet");

    boolean cliTimedOut =
        cmd.exitCode == 1
            && cmd.stdErr.contains(
                "CLI timed out waiting for the job to complete. It's still running on the server.");
    assertTrue(cmd.exitCode == 0 || cliTimedOut, "delete either succeeds or times out");
  }
}
