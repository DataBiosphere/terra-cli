package harness;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Server;
import bio.terra.cli.context.utils.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Utility methods for manipulating the context during testing. */
public class TestContext {

  private TestContext() {}

  /** Delete the global context directory. */
  public static void deleteGlobalContext() throws IOException {
    Path globalContextDir = GlobalContext.getGlobalContextDir();
    if (!globalContextDir.toFile().exists()) {
      return;
    }

    //   - walk the file tree and build a list of paths
    //   - sort the list from longest paths to shortest
    //   - delete the files in this order
    // File.delete only works on file or empty directories
    // since longer paths come first, all children of a directory will be deleted before we get to
    // the directory
    Files.walk(globalContextDir)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  /**
   * Reset the global context: delete the existing directory and setup a new global context for
   * testing. This setup includes logging and setting the server.
   */
  public static void resetGlobalContext() throws IOException {
    deleteGlobalContext();
    GlobalContext globalContext = GlobalContext.get();

    // setup logging for testing (console = OFF, file = DEBUG)
    globalContext.consoleLoggingLevel = Logger.LogLevel.OFF;
    globalContext.fileLoggingLevel = Logger.LogLevel.DEBUG;
    new Logger(globalContext).setupLogging();

    // set the server to the one specified by the test
    // (see the Gradle unitTest task for how this system property gets set from a Gradle property)
    String serverName = System.getProperty("TERRA_SERVER");
    Server.switchTo(serverName);
  }
}
