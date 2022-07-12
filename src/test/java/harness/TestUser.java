package harness;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.GoogleOauth;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test users are defined in testconfig, eg `testconfig/broad.json`. They have varying permissions
 * on the WSM spend profile. These permissions were configured manually, and are not part of the CLI
 * test harness. See CONTRIBUTING.md for more details about the manual setup.
 *
 * <p>This class also includes a {@link #login()} method specifically for testing. Most CLI tests
 * will start with a call to this method to login a test user.
 *
 * <p>This class has several utility methods that randomly choose a test user. The test users are
 * static, so this can help catch errors that are due to some leftover state on a particular test
 * user (e.g. they have some permission that should've been deleted).
 */
public class TestUser {
  private static final Logger logger = LoggerFactory.getLogger(TestUser.class);

  // name of the group that includes CLI test users and has spend profile access
  public static final String CLI_TEST_USERS_GROUP_NAME = "cli-test-users";

  // test users need the cloud platform scope when they talk to GCP directly (e.g. to check the
  // lifecycle property of a GCS bucket, which is not stored as WSM metadata)
  public static final String CLOUD_PLATFORM_SCOPE =
      "https://www.googleapis.com/auth/cloud-platform";

  // See https://medium.com/datamindedbe/mastering-the-google-cloud-platform-sdk-tools-ddcb16b62886
  private static final String GCLOUD_CLIENT_ID = "32555940559.apps.googleusercontent.com";
  private static final String GCLOUD_CLIENT_SECRET = "ZmssLNjJy2998hD4CTg2ejr2";

  public static List<TestUser> getTestUsers() {
    return TestConfig.get().getTestUsers();
  }

  public String email;
  public SpendEnabled spendEnabled;

  /** This enum lists the different ways a user can be enabled on the WSM default spend profile. */
  public enum SpendEnabled {
    OWNER, // owner of the cli-test-users group and owner on the spend profile resource
    NO, // not enabled
    CLI_TEST_USERS_GROUP, // member of cli-test-users group, which is enabled on spend profile
    DIRECTLY; // user of the spend profile resource
  }

  /**
   * This method mimics the typical CLI login flow, in a way that is more useful for testing. It
   * uses domain-wide delegation to populate test user credentials, instead of the usual Google
   * Oauth login flow, which requires manual interaction with a browser.
   *
   * @return global context object, populated with the user's credentials
   */
  public void login(boolean gsutilLogin) throws IOException {
    System.out.println("Logging in test user: " + email);

    // get domain-wide delegated credentials for this user. use the same scopes that are requested
    // of CLI users when they login.
    GoogleCredentials googleCredentials = getCredentials(User.USER_SCOPES);
    writeTerraOAuthCredentialFile(googleCredentials);

    // We're not using pet SA key file (for security reasons), so auth is more complicated.
    if (gsutilLogin) {
      writeAdcCredentialFiles();
      writeGsUtilCredentialFile();
    }

    // unset the current user in the global context if already specified
    Context.setUser(null);

    // do the login flow to populate the global context with the current user
    User.login();
  }

  public void login() throws IOException {
    login(false);
  }

  /** Writes .terra/StoredCredential. */
  private void writeTerraOAuthCredentialFile(GoogleCredentials googleCredentials)
      throws IOException {
    // use the domain-wide delegated credential to build a stored credential for the test user
    StoredCredential dwdStoredCredential = new StoredCredential();
    dwdStoredCredential.setAccessToken(googleCredentials.getAccessToken().getTokenValue());
    dwdStoredCredential.setExpirationTimeMilliseconds(
        googleCredentials.getAccessToken().getExpirationTime().getTime());

    // update the credential store on disk
    // set the single entry to the stored credential for the test user
    DataStore<StoredCredential> dataStore = getCredentialStore();
    dataStore.set(GoogleOauth.CREDENTIAL_STORE_KEY, dwdStoredCredential);
  }

  /** Writes ADC credential files. */
  private void writeAdcCredentialFiles() throws IOException {
    JsonObject json = new JsonObject();
    json.addProperty("client_id", GCLOUD_CLIENT_ID);
    json.addProperty("client_secret", GCLOUD_CLIENT_SECRET);
    json.addProperty("refresh_token", getRefreshToken());
    json.addProperty("type", "authorized_user");

    Path pathForBq =
        Path.of(
            System.getProperty("user.home"), ".config/gcloud/legacy_credentials/default/adc.json");
    Path pathForNextflow =
        Path.of(
            System.getProperty("user.home"), ".config/gcloud/application_default_credentials.json");

    Files.createDirectories(pathForBq.getParent());
    Files.write(pathForBq, json.toString().getBytes(), StandardOpenOption.CREATE);
    Files.write(pathForNextflow, json.toString().getBytes(), StandardOpenOption.CREATE);
  }

  /**
   * Writes ~/.config/gcloud/legacy_credentials/default/.boto that
   * google-cloud-sdk/bin/bootstrapping/gsutil.py expects.
   */
  private void writeGsUtilCredentialFile() throws IOException {
    String fileContent =
        String.format(
            "[OAuth2]\n"
                + "client_id = %s\n"
                + "client_secret = %s\n"
                + "\n"
                + "[Credentials]\n"
                + "gs_oauth2_refresh_token = %s",
            GCLOUD_CLIENT_ID, GCLOUD_CLIENT_SECRET, getRefreshToken());
    Path path =
        Path.of(System.getProperty("user.home"), ".config/gcloud/legacy_credentials/default/.boto");
    Files.createDirectories(path.getParent());
    Files.write(path, fileContent.getBytes(), StandardOpenOption.CREATE);
  }

  /**
   * Get domain-wide delegated Google credentials for this user that include the cloud-platform
   * scope. This is useful for when the test user needs to talk directly to GCP, instead of to WSM
   * or another Terra service.
   */
  public GoogleCredentials getCredentialsWithCloudPlatformScope() throws IOException {
    List<String> scopesWithCloudPlatform = new ArrayList<>(User.USER_SCOPES);
    scopesWithCloudPlatform.add(CLOUD_PLATFORM_SCOPE);
    return getCredentials(scopesWithCloudPlatform);
  }

  /** Get domain-wide delegated Google credentials for this user. */
  private GoogleCredentials getCredentials(List<String> scopes) throws IOException {
    // get a credential for the test-user SA
    Path jsonKey = Path.of("rendered", TestConfig.getTestConfigName(), "test-user-account.json");
    if (!jsonKey.toFile().exists()) {
      throw new FileNotFoundException(
          "Test user SA key file for domain-wide delegation not found. Try re-running tools/render-config.sh. ("
              + jsonKey.toAbsolutePath()
              + ")");
    }
    GoogleCredentials serviceAccountCredential =
        ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey.toFile()))
            .createScoped(scopes);

    // use the test-user SA to get a domain-wide delegated credential for the test user
    GoogleCredentials delegatedUserCredential = serviceAccountCredential.createDelegated(email);
    delegatedUserCredential.refreshIfExpired();
    return delegatedUserCredential;
  }

  /** Helper method that returns a pointer to the credential store on disk. */
  public static DataStore<StoredCredential> getCredentialStore() throws IOException {
    Path globalContextDir = Context.getContextDir();
    FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(globalContextDir.toFile());
    return dataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
  }

  /** Read refresh_token from testconfig. */
  public String getRefreshToken() {
    Path testUserFilePath =
        Paths.get(
            System.getProperty("user.dir"),
            "rendered",
            TestConfig.getTestConfigName(),
            email + ".json");
    logger.debug("Reading test user refresh token from {}", testUserFilePath.toString());

    Gson gson = new Gson();
    Map<String, String> testUserMap = new HashMap<>();
    try {
      Reader reader = Files.newBufferedReader(testUserFilePath);
      testUserMap = gson.fromJson(reader, Map.class);
      reader.close();
    } catch (IOException e) {
      throw new SystemException("Error reading test user file " + testUserFilePath.toString(), e);
    }

    return testUserMap.get("refresh_token");
  }

  /** Returns true if the test user has access to the default WSM spend profile. */
  public boolean hasSpendAccess() {
    return spendEnabled.equals(SpendEnabled.CLI_TEST_USERS_GROUP)
        || spendEnabled.equals(SpendEnabled.DIRECTLY)
        || spendEnabled.equals(SpendEnabled.OWNER);
  }

  /**
   * Randomly chooses a test user, who is anyone except for the given test user. Helpful e.g.
   * choosing a user that is not the workspace creator.
   */
  public static TestUser chooseTestUserWhoIsNot(TestUser testUser) {
    final int maxNumTries = 50;
    for (int ctr = 0; ctr < maxNumTries; ctr++) {
      TestUser chosen = chooseTestUser(Set.of(SpendEnabled.values()));
      if (!chosen.equals(testUser)) {
        return chosen;
      }
    }
    throw new RuntimeException("Error choosing a test user who is anyone except for: " + testUser);
  }

  /** Randomly chooses a test user. */
  public static TestUser chooseTestUser() {
    return chooseTestUser(Set.of(SpendEnabled.values()));
  }

  /** Randomly chooses a test user with spend profile access, but without owner privileges. */
  public static TestUser chooseTestUserWithSpendAccess() {
    return chooseTestUser(
        Set.of(new SpendEnabled[] {SpendEnabled.CLI_TEST_USERS_GROUP, SpendEnabled.DIRECTLY}));
  }

  /**
   * Randomly chooses a test user who is a spend profile admin and an admin of the SAM cli-testers
   * group.
   */
  public static TestUser chooseTestUserWithOwnerAccess() {
    return chooseTestUser(Set.of(SpendEnabled.OWNER));
  }

  /** Randomly chooses a test user without spend profile access. */
  public static TestUser chooseTestUserWithoutSpendAccess() {
    return chooseTestUser(Set.of(SpendEnabled.NO));
  }

  /** Randomly chooses a test user that matches one of the specified spend enabled values. */
  public static TestUser chooseTestUser(Set<SpendEnabled> spendEnabledFilter) {
    // filter the list of all test users to include only those that match one of the specified spend
    // enabled values
    List<TestUser> testUsers =
        TestUser.getTestUsers().stream()
            .filter(testUser -> spendEnabledFilter.contains(testUser.spendEnabled))
            .collect(Collectors.toList());
    if (testUsers.isEmpty()) {
      throw new IllegalArgumentException("No test users match the specified spend enabled values");
    }

    // randomly reorder the list, so we can get a different user each time
    Collections.shuffle(testUsers);
    return testUsers.get(0);
  }
}
