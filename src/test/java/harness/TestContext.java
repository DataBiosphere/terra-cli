package harness;

import bio.terra.cli.businessobject.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;

/** Utility methods for manipulating the context during testing. */
@SuppressFBWarnings(
    value = {"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"},
    justification =
        "Ignore return value of File.delete. This is just a best effort cleanup method. If delete fails, then the next test will likely fail anyway.")
public class TestContext {
  private TestContext() {}

  /**
   * Delete the contents of the global context directory, except for the Java library dependencies
   * in the "lib" sub-directory and the running log files in the "logs" sub-directory.
   */
  public static void clearGlobalContextDir() throws IOException {
    Path globalContextDir = Context.getContextDir();
    if (!globalContextDir.toFile().exists()) {
      return;
    }
    Path globalContextLibDir = globalContextDir.resolve("lib");
    Path globalContextLogsDir = globalContextDir.resolve("logs");

    // delete all children of the global context directory, except for:
    //  - "lib" sub-directory that contains all the JAR dependencies
    //  - "logs" sub-directory that contains the rolling log files
    walkUpFileTree(
        globalContextDir,
        childPath -> {
          if (!childPath.startsWith(globalContextLibDir)
              && !childPath.startsWith(globalContextLogsDir)) {
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
    walkUpFileTree(workingDir, childPath -> childPath.toFile().delete(), true);
  }

  /** Delete the contents of the $HOME/.config/gcloud directory. */
  public static void clearGcloudConfigDirectory() {
    Path gcloudConfigDir = Path.of(System.getProperty("user.home"), ".config/gcloud");
    if (gcloudConfigDir.toFile().exists() && gcloudConfigDir.toFile().isDirectory()) {
      try {
        walkUpFileTree(gcloudConfigDir, childPath -> childPath.toFile().delete(), false);
      } catch (IOException ioEx) {
        System.out.println("Error deleting gcloud config directory: " + ioEx.getMessage());
      }
    }
  }

  /**
   * Walks a file tree from the bottom up, excluding the root. This means that all children of a
   * directory are walked before their parent.
   *
   * <p>Outline of algorithm:
   *
   * <p>- Walk the file tree and build a list of child paths
   *
   * <p>- Sort the list from longest paths to shortest
   *
   * <p>- Process the paths in this order, skipping the root path
   *
   * <p>This is useful for deleting a directory tree. Java's {@link File#delete()} only works on
   * files or empty directories. In this method, since longer paths come first, all children of a
   * directory will be processed before we get to the parent directory.
   *
   * @param root starting node
   * @param processChildPath lambda to process each node
   * @param skipRoot true to skip the root node and only process child nodes
   * @throws IOException IO Exception
   */
  private static void walkUpFileTree(Path root, Consumer<Path> processChildPath, boolean skipRoot)
      throws IOException {
    Files.walk(root)
        .sorted(Comparator.reverseOrder())
        .forEach(
            childPath -> {
              // only process children of the root, not the root itself
              if (!skipRoot || !childPath.equals(root)) {
                processChildPath.accept(childPath);
              }
            });
  }
}
