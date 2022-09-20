package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import harness.TestCommand;
import harness.baseclasses.SingleWorkspaceUnit;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra cromwell` commands. */
@Tag("unit")
public class CromwellConfig extends SingleWorkspaceUnit {
  @Test
  @DisplayName("cromwell generate-config with default dir")
  void cromwellGenerateConfig() throws IOException {
    workspaceCreator.login();

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
    workspaceCreator.login();

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
}