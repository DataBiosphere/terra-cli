package harness;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/** This class holds pointers to the SA that tests can use to create external resources. */
public class TestExternalResources {
  // SA with permission to create/delete/query resources in the project
  private static final String SA_KEY_FILE =
      "./rendered/" + TestConfig.getTestConfigName() + "/external-project-account.json";

  // default scope to request for the SA
  private static final List<String> CLOUD_PLATFORM_SCOPE =
      List.of("https://www.googleapis.com/auth/cloud-platform");

  /** Get credentials for the SA with permissions on the external project. */
  public static GoogleCredentials getSACredentials() throws IOException {
    return ServiceAccountCredentials.fromStream(
            new FileInputStream(TestExternalResources.SA_KEY_FILE))
        .createScoped(TestExternalResources.CLOUD_PLATFORM_SCOPE);
  }
}
