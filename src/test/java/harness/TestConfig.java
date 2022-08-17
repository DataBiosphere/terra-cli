package harness;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.JacksonMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test config that can vary between Terra deployments. */
public final class TestConfig {
  private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);
  private static final String TESTCONFIGS_RESOURCE_DIRECTORY = "testconfigs";
  private static TestConfig INSTANCE;
  @JsonProperty private List<TestUser> testUsers;
  // Some CLI tests directly create external resources, eg FineGrainedAccessGcsObjectReference.java
  @JsonProperty private String projectForExternalResources;
  // Whether to use janitor to clean up external resources and workspaces.
  @JsonProperty private boolean useJanitor;
  // The PubSub topic to use for publishing Janitor cleanup requests
  @JsonProperty private String janitorPubSubTopic;
  // The project ID of the Janitor PubSub topic
  @JsonProperty private String janitorPubSubProjectId;

  private TestConfig() {}

  // Returns name of file under `testconfigs/` without `.json`, eg `broad`. This is also name of the
  // directory under `rendered` where credentials are stored.
  public static String getTestConfigName() {
    return System.getProperty("TERRA_TEST_CONFIG_NAME");
  }

  public static TestConfig get() {
    if (INSTANCE == null) {
      String testConfigFileName = getTestConfigName() + ".json";
      if (testConfigFileName == null || testConfigFileName.isEmpty()) {
        throw new SystemException("TERRA_TEST_CONFIG_FILE_NAME must be set");
      }
      TestConfig testConfig = fromJsonFile(testConfigFileName);
      validateTestConfig(testConfig, testConfigFileName);
      INSTANCE = testConfig;
    }
    return INSTANCE;
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
      String filePath = TESTCONFIGS_RESOURCE_DIRECTORY + "/" + fileName;
      logger.debug("Reading test config from {}", filePath);
      InputStream inputStream = FileUtils.getResourceFileHandle(filePath);
      testConfig = JacksonMapper.getMapper().readValue(inputStream, TestConfig.class);
    } catch (FileNotFoundException fnfEx) {
      throw new SystemException(
          String.format(
              "Test config file %s not found in src/main/resources/tests. Please populate it (as"
                  + " well a directory under rendered/ for your deployment).",
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
    if (testConfig.useJanitor) {
      Preconditions.checkState(
          testConfig.janitorPubSubProjectId != null && testConfig.janitorPubSubTopic != null,
          "In %s, if useJanitor is set, janitorPubSubProjectId and janitorPubSubTopic must also be set.",
          testConfigFileName);
    }
  }

  // ====================================================
  // Property getters.
  public List<TestUser> getTestUsers() {
    return testUsers;
  }

  public String getProjectForExternalResources() {
    return projectForExternalResources;
  }

  public boolean useJanitor() {
    return useJanitor;
  }

  public String getJanitorPubSubTopic() {
    return janitorPubSubTopic;
  }

  public String getJanitorPubSubProjectId() {
    return janitorPubSubProjectId;
  }
}
