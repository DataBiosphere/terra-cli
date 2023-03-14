package unit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import harness.TestCommand;
import harness.TestCommand.Result;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra config` commands specific to CloudPlatform.GCP. */
@Tag("unit-gcp")
public class ConfigGcp extends SingleWorkspaceUnit {
  @Test
  @DisplayName("app-launch config affects how apps are launched")
  void appLaunch() throws IOException {
    String badImageError = "No such image: badimageid";

    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // set the docker image id to an invalid string
    TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=badimageid");

    // `terra config set app-launch LOCAL_PROCESS`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "LOCAL_PROCESS");

    // Apps launched via local process should not be affected
    Result cmd = TestCommand.runCommand("app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    // Cmd exit code can either be 0=success or 1 because gcloud fails with
    // `(gcloud.config.get-value) Failed to create the default configuration. Ensure your have
    // the
    // correct permissions on`.
    assertThat(
        "Expected to return exit code 0 or 1, instead got " + cmd.exitCode,
        cmd.exitCode == 0 || cmd.exitCode == 1);
    assertThat(
        "bad docker image should not affect local mode",
        cmd.stdErr,
        not(containsString(badImageError)));

    // `terra config set app-launch DOCKER_CONTAINER`
    TestCommand.runCommandExpectSuccess("config", "set", "app-launch", "DOCKER_CONTAINER");

    // Apps launched via docker container should error out
    String stdErr =
        TestCommand.runCommandExpectExitCode(3, "app", "execute", "echo", "$GOOGLE_CLOUD_PROJECT");
    assertThat("docker image not found error returned", stdErr, containsString(badImageError));
  }
}
