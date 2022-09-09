package unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.SingleWorkspaceUnit;
import harness.utils.WorkspaceUtils;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Tests for the `terra cromwell` commands. */
@Tag("unit")
public class CromwellConfig extends SingleWorkspaceUnit {
  @Test
  @DisplayName("cromwell config create in root path")
  void cromwellConfigCreateRoot() throws IOException {
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();
    WorkspaceUtils.createWorkspace(testUser);

    // `terra cromwell generate-config`
    TestCommand.runCommandExpectSuccess("cromwell", "generate-config");

    // New cromwell.conf file generate successfully.
    assertTrue(new File("cromwell.conf").isFile());

    // Remove the created config file.
    new File("cromwell.conf").delete();

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }

  @Test
  @DisplayName("cromwell config create in test path")
  void cromwellConfigCreatePath() throws IOException {
    TestUser testUser = TestUser.chooseTestUserWithSpendAccess();
    testUser.login();
    WorkspaceUtils.createWorkspace(testUser);

    // `terra cromwell generate-config --path=build/cromwell.conf`
    TestCommand.runCommandExpectSuccess(
        "cromwell", "generate-config", "--path=build/cromwell.conf");

    // New cromwell.conf file generate successfully.
    assertTrue(
        new File("./build/cromwell.conf").isFile(), "New cromwell.conf file generate successfully");

    // Remove the created config file.
    new File("./build/cromwell.conf").delete();

    // `terra workspace delete`
    TestCommand.runCommandExpectSuccess("workspace", "delete", "--quiet");
  }
}
