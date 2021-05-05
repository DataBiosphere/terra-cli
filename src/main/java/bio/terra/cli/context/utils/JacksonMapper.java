package bio.terra.cli.context.utils;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for using Jackson to de/serialize JSON. This class maintains a singleton instance
 * of the Jackson {@link ObjectMapper}, to avoid re-loading the modules multiple times for a single
 * CLI command.
 */
public class JacksonMapper {
  private static final Logger logger = LoggerFactory.getLogger(JacksonMapper.class);

  private static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  /** Getter for the singleton instance of the default Jackson {@link ObjectMapper} instance. */
  public static ObjectMapper getMapper() {
    return objectMapper;
  }

  /**
   * Getter for an instance of the Jackson {@link ObjectMapper}, with the specified Jackson features
   * enabled. If no Jackson features are specified (i.e. the list of mapper featuers is empty), then
   * this method is equivalent to the {@link #getMapper()} method.
   */
  public static ObjectMapper getMapper(List<MapperFeature> mapperFeatures) {
    // if no Jackson features are specified, just return the default mapper object
    if (mapperFeatures.size() == 0) {
      return getMapper();
    }

    // create a copy of the default mapper and enable any Jackson features specified
    ObjectMapper objectMapperWithFeatures = getMapper().copy();
    for (MapperFeature mapperFeature : mapperFeatures) {
      objectMapperWithFeatures.enable(mapperFeature);
    }
    return objectMapperWithFeatures;
  }

  /**
   * Read a JSON-formatted file into a Java object using the Jackson object mapper.
   *
   * @param inputFile the file to read in
   * @param javaObjectClass the Java object class
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws FileNotFoundException if the file to read in does not exist
   */
  public static <T> T readFileIntoJavaObject(File inputFile, Class<T> javaObjectClass)
      throws IOException {
    return readFileIntoJavaObject(inputFile, javaObjectClass, Collections.emptyList());
  }

  /**
   * Read a JSON-formatted file into a Java object using the Jackson object mapper.
   *
   * @param inputFile the file to read in
   * @param javaObjectClass the Java object class
   * @param mapperFeatures list of Jackson mapper features to enable
   * @param <T> the Java object class to map the file contents to
   * @return an instance of the Java object class
   * @throws FileNotFoundException if the file to read in does not exist
   */
  public static <T> T readFileIntoJavaObject(
      File inputFile, Class<T> javaObjectClass, List<MapperFeature> mapperFeatures)
      throws IOException {
    // use Jackson to map the file contents to an instance of the specified class
    ObjectMapper objectMapper = getMapper(mapperFeatures);

    // enable any Jackson features specified
    for (MapperFeature mapperFeature : mapperFeatures) {
      objectMapper.enable(mapperFeature);
    }

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
    ObjectMapper objectMapper = getMapper();
    ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

    // create the file and any parent directories if they don't already exist
    FileUtils.createFile(outputFile);

    logger.debug("Serializing object with Jackson to file: {}", outputFile.getAbsolutePath());
    objectWriter.writeValue(outputFile, javaObject);
  }
}
