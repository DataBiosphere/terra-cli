package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.File;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for the `terra cromwell` commands. */
// TODO(PF-2333): This is currently broken due to a gcloud/GHA runner interaction
// @Tag("unit")
@Disabled
public class CromwellConfig extends SingleWorkspaceUnit {
  @Test
  @DisplayName("cromwell generate-config with default dir")
  void cromwellGenerateConfig() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config --google-bucket-name=foo`
    TestCommand.runCommandExpectSuccess("cromwell", "generate-config", "--google-bucket-name=foo");

    // New cromwell.conf file generate successfully.
    assertTrue(new File("cromwell.conf").isFile());

    // Remove the created config file.
    new File("cromwell.conf").delete();
  }

  @Test
  @DisplayName("cromwell generate-config with custom dir")
  void cromwellGenerateConfigCustomDir() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config --dir=build --google-bucket-name=foo`
    TestCommand.runCommandExpectSuccess(
        "cromwell", "generate-config", "--dir=build", "--google-bucket-name=foo");

    // New cromwell.conf file generate successfully.
    assertTrue(
        new File("./build/cromwell.conf").isFile(), "New cromwell.conf file generate successfully");

    // Remove the created config file.
    new File("./build/cromwell.conf").delete();
  }

  @Test
  @DisplayName("cromwell generate-config check errors with bucket params")
  void cromwellGenerateConfigBucketParamErrors() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config`
    String stdErr = TestCommand.runCommandExpectExitCode(2, "cromwell", "generate-config");
    assertThat(
        "error message indicates missing required argument",
        stdErr,
        CoreMatchers.containsString("Missing required argument (specify one of these):"));

    // `terra cromwell generate-config --workspace-bucket-name=foo --google-bucket-name=foo2`
    stdErr =
        TestCommand.runCommandExpectExitCode(
            2,
            "cromwell",
            "generate-config",
            "--workspace-bucket-name=foo",
            "--google-bucket-name=foo2");
    assertThat(
        "error message indicates only one must be specified",
        stdErr,
        CoreMatchers.containsString("are mutually exclusive (specify only one)"));
  }
}
