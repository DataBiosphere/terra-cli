package harness;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.auth.GoogleCredentialUtils;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This enum lists the test users available for CLI tests. They have varying permissions on the WSM
 * spend profile. These permissions were configured manually, and are not part of the CLI test
 * harness. See CONTRIBUTING.md for more details about the manual setup.
 *
 * <p>This class also includes a {@link #login(GlobalContext)} method specifically for testing. Most
 * CLI tests will start with a call to this method to login a test user.
 */
public enum TestUsers {
  PENELOPE_TWILIGHTSHAMMER("Penelope.TwilightsHammer@test.firecloud.org", SpendEnabled.OWNER),
  JOHN_WHITECLAW("John.Whiteclaw@test.firecloud.org", SpendEnabled.CLI_TEST_USERS_GROUP),
  LILY_SHADOWMOON("Lily.Shadowmoon@test.firecloud.org", SpendEnabled.CLI_TEST_USERS_GROUP),
  BROOKLYN_THUNDERLORD("Brooklyn.Thunderlord@test.firecloud.org", SpendEnabled.DIRECTLY),
  NOAH_FROSTWOLF("Noah.Frostwolf@test.firecloud.org", SpendEnabled.DIRECTLY),
  ETHAN_BONECHEWER("Ethan.Bonechewer@test.firecloud.org", SpendEnabled.NO);

  public final String email;
  public final SpendEnabled spendEnabled;

  TestUsers(String email, SpendEnabled spendEnabled) {
    this.email = email;
    this.spendEnabled = spendEnabled;
  }

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
   */
  public void login(GlobalContext globalContext) throws IOException {
    // get a credential for the test-user SA
    Path jsonKey = Path.of("rendered", "test-user-account.json");
    if (!jsonKey.toFile().exists()) {
      throw new FileNotFoundException(
          "Test user SA key file for domain-wide delegation not found. Try re-running tools/render-config.sh. ("
              + jsonKey.toAbsolutePath()
              + ")");
    }
    GoogleCredentials serviceAccountCredential =
        ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey.toFile()))
            .createScoped(AuthenticationManager.SCOPES);

    // use the test-user SA to get a domain-wide delegated credential for the test user
    GoogleCredentials delegatedUserCredential = serviceAccountCredential.createDelegated(email);
    delegatedUserCredential.refreshIfExpired();

    // use the domain-wide delegated credential to build a stored credential for the test user
    StoredCredential dwdStoredCredential = new StoredCredential();
    dwdStoredCredential.setAccessToken(delegatedUserCredential.getAccessToken().getTokenValue());
    dwdStoredCredential.setExpirationTimeMilliseconds(
        delegatedUserCredential.getAccessToken().getExpirationTime().getTime());

    // update the credential store on disk
    // set the single entry to the stored credential for the test user
    DataStore<StoredCredential> dataStore = getCredentialStore();
    dataStore.set(GoogleCredentialUtils.CREDENTIAL_STORE_KEY, dwdStoredCredential);

    // unset the current user in the global context if already specified
    globalContext.unsetCurrentTerraUser();

    // do the login flow to populate the global context with the current user
    new AuthenticationManager(globalContext, WorkspaceContext.readFromFile()).loginTerraUser();
  }

  /** Utility method to logout the current user. */
  public void logout(GlobalContext globalContext) {
    new AuthenticationManager(globalContext, WorkspaceContext.readFromFile()).logoutTerraUser();
  }

  /** Helper method that returns a pointer to the credential store on disk. */
  public static DataStore<StoredCredential> getCredentialStore() throws IOException {
    Path globalContextDir = GlobalContext.getGlobalContextDir();
    FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(globalContextDir.toFile());
    return dataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
  }

  /** Randomly chooses a test user. */
  public static TestUsers chooseTestUser() {
    return chooseTestUser(Set.of(SpendEnabled.values()));
  }

  /** Randomly chooses a test user with spend profile access. */
  public static TestUsers chooseTestUserWithSpendAccess() {
    return chooseTestUser(
        Set.of(new SpendEnabled[] {SpendEnabled.CLI_TEST_USERS_GROUP, SpendEnabled.DIRECTLY}));
  }

  /** Randomly chooses a test user that matches one of the specified spend enabled values. */
  public static TestUsers chooseTestUser(Set<SpendEnabled> spendEnabledFilter) {
    // filter the list of all test users to include only those that match one of the specified spend
    // enabled values
    List<TestUsers> testUsers =
        Arrays.asList(TestUsers.values()).stream()
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
