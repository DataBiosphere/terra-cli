package bio.terra.cli.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating files on disk. */
public class FileUtils {

  private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

  /**
   * Build a stream handle to a resource file.
   *
   * @return the new file handle
   * @throws FileNotFoundException if the resource file doesn't exist
   */
  public static InputStream getResourceFileHandle(String resourceFilePath)
      throws FileNotFoundException {
    InputStream inputStream =
        FileUtils.class.getClassLoader().getResourceAsStream(resourceFilePath);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource file not found: " + resourceFilePath);
    }
    return inputStream;
  }

  /** Create the file and any parent directories if they don't already exist. */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in the calling method if the mkdirs or createNewFile calls fail.")
  public static void createFile(File file) throws IOException {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
  }

  /**
   * Write a string directly to a file.
   *
   * @param outputFile the file to write to
   * @param fileContents the string to write
   * @return the file that was written to
   */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in this same method if the mkdirs or createNewFile calls fail.")
  public static Path writeStringToFile(File outputFile, @Nullable String fileContents)
      throws IOException {
    logger.debug("Writing to file: {}", outputFile.getAbsolutePath());

    // create the file and any parent directories if they don't already exist
    createFile(outputFile);

    return Files.writeString(outputFile.toPath(), fileContents == null ? "" : fileContents);
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
  public static void walkUpFileTree(Path root, Consumer<Path> processChildPath, boolean skipRoot)
      throws IOException {
    try (Stream<Path> paths = Files.walk(root)) {
      paths
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

  /**
   * Creates directories for a path if it does not exist.
   *
   * @param path The path to create directories for.
   * @throws SystemException If the creation of the directories failed.
   */
  public static void createDirectories(Path path) throws SystemException {
    if (!path.toFile().exists() && !path.toFile().mkdirs()) {
      throw new SystemException("Failed to create directory: " + path);
    }
  }

  /**
   * Helper method to check if a directory is empty.
   *
   * @param path The path to check.
   * @return True if the path is a directory and is empty, false otherwise.
   */
  public static boolean isEmptyDirectory(Path path) {
    try {
      return Files.isDirectory(path) && Objects.requireNonNull(path.toFile().list()).length == 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Recursively deletes empty subdirectories in the given root directory, excluding the root
   * itself.
   *
   * @param root the root directory to delete empty subdirectories from.
   * @throws SystemException if an error occurs while deleting the empty directories.
   */
  public static void deleteEmptyDirectories(Path root) {
    try {
      FileUtils.walkUpFileTree(
          root,
          childPath -> {
            if (isEmptyDirectory(childPath)) {
              childPath.toFile().delete();
            } else {
              throw new UserActionableException(
                  String.format("Directory is not empty: %s", childPath));
            }
          },
          false);
    } catch (Exception ex) {
      throw new SystemException(String.format("Error deleting empty directory %s", root), ex);
    }
  }
}
