package bio.terra.cli.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating files on disk. */
public class FileUtils {
  private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

  /**
   * Read a JSON-formatted file into a Java object using the Jackson object mapper.
   *
   * @param inputFile the file to read in
   * @param javaObjectClass the Java object class
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   */
  public static <T> T readFileIntoJavaObject(File inputFile, Class<T> javaObjectClass)
      throws IOException {
    // get a reference to the file
    if (!inputFile.exists()) {
      return null;
    }

    // use Jackson to map the file contents to an instance of the specified class
    ObjectMapper objectMapper = new ObjectMapper();
    try (FileInputStream inputStream = new FileInputStream(inputFile)) {
      return objectMapper.readValue(inputStream, javaObjectClass);
    }
  }

  /**
   * Write a Java object to a JSON-formatted file using the Jackson object mapper.
   *
   * @param outputFile the file to write to
   * @param javaObject the Java object to write
   * @param <T> the Java object class to write
   * @return an instance of the Java object class
   */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in this same method if the mkdirs or createNewFile calls fail.")
  public static <T> void writeJavaObjectToFile(File outputFile, T javaObject) throws IOException {
    // use Jackson to map the object to a JSON-formatted text block
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

    // create the file and any parent directories if they don't already exist
    createFile(outputFile);

    logger.debug("Serializing object with Jackson to file: {}", outputFile.getAbsolutePath());
    objectWriter.writeValue(outputFile, javaObject);
  }

  /**
   * Write a string directly to a file.
   *
   * @param directory the directory where the file is
   * @param fileName the file name
   * @param fileContents the string to write
   * @return the file that was written to
   */
  @SuppressFBWarnings(
      value = "RV_RETURN_VALUE_IGNORED",
      justification =
          "A file not found exception will be thrown anyway in this same method if the mkdirs or createNewFile calls fail.")
  public static Path writeStringToFile(Path directory, String fileName, String fileContents)
      throws IOException {
    // get a reference to the file
    Path outputPath = directory.resolve(fileName);
    logger.debug("Writing to file: {}", outputPath.toAbsolutePath());

    // create the file and any parent directories if they don't already exist
    File outputFile = outputPath.toFile();
    createFile(outputFile);

    return Files.write(
        directory.resolve(fileName), fileContents.getBytes(Charset.forName("UTF-8")));
  }

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
  public static void createFile(File file) throws IOException {
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
  }
}
