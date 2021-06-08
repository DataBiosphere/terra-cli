package harness;

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
  public static final String gcpProjectId = "terra-cli-dev";

  // SA with permission to create/delete/query resources in the project
  public static final String saKeyFile = "./rendered/ci-account.json";

  // default scope to request for the SA
  public static final List<String> cloudPlatformScope =
      Collections.unmodifiableList(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
}
