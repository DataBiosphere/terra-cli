package harness;

import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.common.cleanup.CleanupConfig;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class holds pointers to the hard-coded configuration for CRL Janitor that tests can use to
 * create external resources that will get automatically cleaned up if e.g. the tests fail.
 */
public class CRLJanitor {
  // CRL janitor client SA
  private static final String SA_KEY_FILE = "./rendered/janitor-client.json";

  // default scope to request for the SA
  private static final List<String> CLOUD_PLATFORM_SCOPE =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

  private static final String DEFAULT_CLIENT_NAME = "cli-test";

  private static final CleanupConfig DEFAULT_CLEANUP_CONFIG =
      CleanupConfig.builder()
          .setTimeToLive(Duration.ofHours(2))
          .setCleanupId("cli-test-" + System.getProperty("TEST_RUN_ID"))
          .setCredentials(CRLJanitor.getSACredentials())
          .setJanitorTopicName("crljanitor-tools-pubsub-topic")
          .setJanitorProjectId("terra-kernel-k8s")
          .build();

  // default client configuration for CRL Janitor (cleanup mode)
  public static final ClientConfig DEFAULT_CLIENT_CONFIG =
      ClientConfig.Builder.newBuilder()
          .setClient(DEFAULT_CLIENT_NAME)
          .setCleanupConfig(DEFAULT_CLEANUP_CONFIG)
          .build();

  /** Get credentials for the Janitor client SA. */
  private static GoogleCredentials getSACredentials() {
    try {
      return ServiceAccountCredentials.fromStream(new FileInputStream(SA_KEY_FILE))
          .createScoped(CLOUD_PLATFORM_SCOPE);
    } catch (IOException ioEx) {
      throw new RuntimeException("Error reading SA credentials for Janitor client.", ioEx);
    }
  }
}
