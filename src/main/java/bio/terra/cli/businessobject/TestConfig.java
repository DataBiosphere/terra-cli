package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.JacksonMapper;
import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test config. */
public class TestConfig {
  private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

  // Some CLI tests directly create external resources, eg FineGrainedAccessGcsObjectReference.java
  private String projectForExternalResources;

  // Whether to use janitor to clean up external resources. For more about terra-cli and Janitor,
  // see PF-886.
  private boolean useJanitorForExternalResourcesCreatedByTests;

  public static final String TESTS_RESOURCE_DIRECTORY = "tests";

  public static TestConfig get() {
    String testConfigFileName = System.getenv("TERRA_TEST_CONFIG_FILE_NAME");
    if (testConfigFileName == null || testConfigFileName.isEmpty()) {
      throw new SystemException("TERRA_TEST_CONFIG_FILE_NAME must be set");
    }
    TestConfig testConfig = fromJsonFile(testConfigFileName);
    validateTestConfig(testConfig, testConfigFileName);
    return testConfig;
  }

  /**
   * Reads test config from a file in src/main/resources/tests.
   *
   * @param fileName file name
   * @return an instance of this class
   */
  private static TestConfig fromJsonFile(String fileName) {
    TestConfig testConfig;
    try {
      String filePath = TESTS_RESOURCE_DIRECTORY + "/" + fileName;
      logger.debug("Reading test config from {}", filePath);
      InputStream inputStream =
          FileUtils.getResourceFileHandle(TESTS_RESOURCE_DIRECTORY + "/" + fileName);
      testConfig = JacksonMapper.getMapper().readValue(inputStream, TestConfig.class);
    } catch (FileNotFoundException fnfEx) {
      throw new SystemException(
          String.format(
              "Test config file %s not found in src/main/resources/tests. You may need to run render-verily-config.sh.",
              fileName),
          fnfEx);
    } catch (IOException ioEx) {
      throw new SystemException(
          String.format("Error reading in test config file: src/main/resources/tests/%s", fileName),
          ioEx);
    }
    return testConfig;
  }

  private static void validateTestConfig(TestConfig testConfig, String testConfigFileName) {
    Preconditions.checkState(
        testConfig.getProjectForExternalResources() != null
            && !testConfig.getProjectForExternalResources().isEmpty(),
        "In %s, projectForExternalResources must be set",
        testConfigFileName);
  }

  // ====================================================
  // Property getters.
  public String getProjectForExternalResources() {
    return projectForExternalResources;
  }

  public boolean getUseJanitorForExternalResourcesCreatedByTests() {
    return useJanitorForExternalResourcesCreatedByTests;
  }
}
