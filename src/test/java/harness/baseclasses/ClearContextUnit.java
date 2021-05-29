package harness.baseclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.utils.Logger;
import harness.TestCommand;
import harness.TestContext;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for unit tests that includes standard setup/cleanup. Because they are for unit tests,
 * these methods call the setup/cleanup commands directly in Java.
 */
public class ClearContextUnit {
  @BeforeEach
  protected void setupEachTime() throws IOException {
    TestContext.clearGlobalContextDir();
    resetGlobalContext();
  }

  /**
   * Reset the global context for a unit test. This setup includes logging, setting the server, and
   * setting the docker image id.
   */
  private static void resetGlobalContext() {
    // setup logging for testing (console = OFF, file = DEBUG)
    TestCommand.Result cmd =
        TestCommand.runCommand("config", "set", "logging", "--console", "--level=OFF");
    assertEquals(0, cmd.exitCode);
    cmd = TestCommand.runCommand("config", "set", "logging", "--file", "--level=DEBUG");
    assertEquals(0, cmd.exitCode);

    // also update the logging directly in this process, because the config commands only affect
    // future processes
    GlobalContext globalContext = GlobalContext.get();
    new Logger(globalContext).setupLogging();

    // logout the current user
    globalContext.getCurrentTerraUser().ifPresent(terraUser -> terraUser.logout());

    // set the server to the one specified by the test
    // (see the Gradle test task for how this env var gets set from a Gradle property)
    cmd = TestCommand.runCommand("server", "set", "--name", System.getenv("TERRA_SERVER"));
    assertEquals(0, cmd.exitCode);

    // set the docker image id to the default
    cmd = TestCommand.runCommand("config", "set", "image", "--default");
    assertEquals(0, cmd.exitCode);
  }
}
