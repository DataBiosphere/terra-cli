package harness.baseclasses;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.utils.Logger;
import harness.TestCommand;
import harness.TestContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

/**
 * Base class for unit tests that includes standard setup/cleanup. Because they are for unit tests,
 * these methods call the setup/cleanup commands directly in Java.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClearContextUnit {
  @BeforeEach
  /**
   * Clear the context before each test method. For sub-classes, it's best to call this at the end
   * of the setupEachTime method so that each test method starts off with a clean context.
   */
  protected void setupEachTime() throws IOException {
    // TODO: clear gcloud config dir
    Path gcloudConfigDir = Path.of(System.getProperty("user.home"), ".config/gcloud");
    if (gcloudConfigDir.toFile().exists() && gcloudConfigDir.toFile().isDirectory()) {
      Files.walk(gcloudConfigDir)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
    TestContext.clearGlobalContextDir();
    resetContext();
  }

  /**
   * Reset the global context for a unit test. This setup includes logging, setting the server, and
   * setting the docker image id.
   */
  public static void resetContext() {
    // setup logging for testing (console = OFF, file = DEBUG)
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--console", "--level=OFF");
    TestCommand.runCommandExpectSuccess("config", "set", "logging", "--file", "--level=DEBUG");

    // also update the logging directly in this process, because the config commands only affect
    // future processes
    Context.initializeFromDisk();
    Logger.setupLogging(
        Context.getConfig().getConsoleLoggingLevel(), Context.getConfig().getFileLoggingLevel());

    // logout the current user
    Context.getUser().ifPresent(User::logout);

    // set the server to the one specified by the test
    // (see the Gradle test task for how this env var gets set from a Gradle property)
    TestCommand.runCommandExpectSuccess("server", "set", "--name", System.getenv("TERRA_SERVER"));

    // set the docker image id to the one specified by the test, or to the default if it's
    // unspecified
    String dockerImageEnvVar = System.getenv("TERRA_DOCKER_IMAGE");
    if (dockerImageEnvVar != null && !dockerImageEnvVar.isEmpty()) {
      TestCommand.runCommandExpectSuccess("config", "set", "image", "--image=" + dockerImageEnvVar);
    } else {
      TestCommand.runCommandExpectSuccess("config", "set", "image", "--default");
    }
  }
}
