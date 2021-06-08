package harness;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class holds pointers to the hard-coded GCP project and SA that tests can use to create
 * external resources.
 */
public class TestExternalResources {
  // TODO (PF-829): change this project id and SA key file to point to ones Terraformed specifically
  // for testing

  // Google project to create resources in
  private static final String gcpProjectId = "terra-cli-dev";

  // SA with permission to create/delete/query resources in the project
  private static final String saKeyFile = "./rendered/ci-account.json";

  // default scope to request for the SA
  private static final List<String> cloudPlatformScope =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

  /** Get credentials for the SA with permissions on the external project. */
  public static GoogleCredentials getSACredentials() throws IOException {
    return ServiceAccountCredentials.fromStream(
            new FileInputStream(TestExternalResources.saKeyFile))
        .createScoped(TestExternalResources.cloudPlatformScope);
  }

  public static String getProjectId() {
    return gcpProjectId;
  }
}
