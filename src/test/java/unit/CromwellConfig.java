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
  @DisplayName("app-launch config affects how apps are launched")
  void cromwellConfig() throws IOException {
    workspaceCreator.login();

    // `terra workspace set --id=$id`
    TestCommand.runCommandExpectSuccess("workspace", "set", "--id=" + getUserFacingId());

    // `terra cromwell generate-config`
    TestCommand.runCommandExpectSuccess("cromwell", "generate-config");

    // New cromwell.conf file exist.
    assertTrue(new File("cromwell.conf").isFile());

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
