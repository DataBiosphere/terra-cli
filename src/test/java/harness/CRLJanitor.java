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
  private static final String SA_KEY_FILE =
      "./rendered/" + TestConfig.getTestConfigName() + "/janitor-client.json";

  // default scope to request for the SA
  private static final List<String> CLOUD_PLATFORM_SCOPE =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

  private static final String DEFAULT_CLIENT_NAME = "cli-test";

  public static final ClientConfig getClientConfig() {
    ClientConfig.Builder builder = ClientConfig.Builder.newBuilder().setClient(DEFAULT_CLIENT_NAME);
    if (TestConfig.get().getUseJanitorForExternalResourcesCreatedByTests()) {
      builder.setCleanupConfig(
          CleanupConfig.builder()
              .setTimeToLive(Duration.ofHours(2))
              .setCleanupId("cli-test-" + System.getProperty("TEST_RUN_ID"))
              .setCredentials(CRLJanitor.getSACredentials())
              // TODO(PF-963): As part of setting up janitor for Verily, move to test config
              .setJanitorTopicName("crljanitor-tools-pubsub-topic")
              .setJanitorProjectId("terra-kernel-k8s")
              .build());
    }
    return builder.build();
  }

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
