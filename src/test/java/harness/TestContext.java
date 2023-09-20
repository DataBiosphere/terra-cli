package harness;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.utils.FileUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Path;

/** Utility methods for manipulating the context during testing. */
@SuppressFBWarnings(
    value = {"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"},
    justification =
        "Ignore return value of File.delete. This is just a best effort cleanup method. If delete fails, then the next test will likely fail anyway.")
public class TestContext {
  private TestContext() {}

  /**
   * Delete the contents of the global context directory, except for the Java library dependencies
   * in the "lib" sub-directory, the running log files in the "logs" sub-directory and the
   * aws-workspace configuration in the "aws" sub-directory
   */
  public static void clearGlobalContextDir() throws IOException {
    Path globalContextDir = Context.getContextDir();
    if (!globalContextDir.toFile().exists()) {
      return;
    }
    Path globalContextLibDir = globalContextDir.resolve("lib");
    Path globalContextLogsDir = globalContextDir.resolve("logs");
    Path globalContextAwsDir = globalContextDir.resolve("aws");

    // delete all children of the global context directory, except for:
    //  - "lib" sub-directory that contains all the JAR dependencies
    //  - "logs" sub-directory that contains the rolling log files
    //  - "aws" sub-directory that contains the workspace aws configuration (created and deleted by
    // the workspace)
    FileUtils.walkUpFileTree(
        globalContextDir,
        childPath -> {
          if (!childPath.startsWith(globalContextLibDir)
              && !childPath.startsWith(globalContextLogsDir)
              && !childPath.startsWith(globalContextAwsDir)) {
            childPath.toFile().delete();
          }
        },
        true);
  }

  /** Delete the contents of the working directory. */
  public static void clearWorkingDirectory() throws IOException {
    Path workingDir = Path.of(System.getProperty("TERRA_WORKING_DIR")).toAbsolutePath();
    if (!workingDir.toFile().exists()) {
      return;
    }

    // delete all children of the working directory
    FileUtils.walkUpFileTree(workingDir, childPath -> childPath.toFile().delete(), true);
  }

  /** Delete the contents of the $HOME/.config/gcloud directory. */
  public static void clearGcloudConfigDirectory() {
    Path gcloudConfigDir = Path.of(System.getProperty("user.home"), ".config/gcloud");
    if (gcloudConfigDir.toFile().exists() && gcloudConfigDir.toFile().isDirectory()) {
      try {
        FileUtils.walkUpFileTree(gcloudConfigDir, childPath -> childPath.toFile().delete(), false);
      } catch (IOException ioEx) {
        System.out.println("Error deleting gcloud config directory: " + ioEx.getMessage());
      }
    }
  }
}
