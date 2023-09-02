package harness;

import bio.terra.cli.businessobject.User;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;

/** This class holds pointers to the SA that tests can use to create external resources. */
public class TestExternalResources {
  // SA with permission to create/delete/query resources in the project
  private static final String SA_KEY_FILE = "./rendered/external-project-account.json";

  /** Get credentials for the SA with permissions on the external project. */
  public static GoogleCredentials getSACredentials() throws IOException {
    return ServiceAccountCredentials.fromStream(new FileInputStream(SA_KEY_FILE))
        .createScoped(User.CLOUD_PLATFORM_SCOPES);
  }
}
