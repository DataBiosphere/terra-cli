package harness.baseclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.businessobject.Context;
import harness.TestBashScript;
import harness.TestContext;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for integration tests that includes standard setup/cleanup. Because they are for
 * integration tests, these methods call the setup/cleanup commands from a bash script.
 */
public class ClearContextIntegration {
  @BeforeEach
  protected void setupEachTime() throws IOException {
    TestContext.clearGcloudConfigDirectory();
    TestContext.clearGlobalContextDir();
    TestContext.clearWorkingDirectory();

    // run a script that resets the global context
    int exitCode = TestBashScript.runScript("SetupContext.sh");
    assertEquals(0, exitCode, "SetupContext script completed without errors");

    // initialize the Context from disk, so we can login with TestUser
    Context.initializeFromDisk();
  }

  @AfterEach
  protected void cleanupEachTime() {
    // run a script that deletes the current workspace, but don't fail the test for any errors
    // during cleanup
    TestBashScript.runScript("DeleteWorkspace.sh");
  }
}
