package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.auth.GoogleCredentialUtils;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.utils.SamService;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import harness.TestCommand;
import harness.TestUsers;
import harness.baseclasses.ClearContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for the authentication part of the test harness, and the state of the credential store on
 * disk.
 */
@Tag("unit")
public class AuthLoginLogout extends ClearContext {
  @Test
  @DisplayName("test user login updates global context")
  void loginTestUser() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUser();
    testUser.login(GlobalContext.readFromFile());

    // check that the credential exists in the store on disk
    DataStore<StoredCredential> dataStore = TestUsers.getCredentialStore();
    assertEquals(1, dataStore.keySet().size(), "credential store only contains one entry");
    assertTrue(
        dataStore.containsKey(GoogleCredentialUtils.CREDENTIAL_STORE_KEY),
        "credential store contains hard-coded single user key");
    StoredCredential storedCredential = dataStore.get(GoogleCredentialUtils.CREDENTIAL_STORE_KEY);
    assertThat(storedCredential.getAccessToken(), CoreMatchers.not(emptyOrNullString()));

    // check that the current user in the global context = the test user
    // read the global context in from disk again to make sure it got persisted
    GlobalContext globalContext = GlobalContext.readFromFile();
    Optional<TerraUser> currentTerraUser = globalContext.getCurrentTerraUser();
    assertTrue(currentTerraUser.isPresent(), "current user set in global context");
    assertThat(
        "test user email matches the current user set in global context",
        testUser.email,
        equalToIgnoringCase(currentTerraUser.get().terraUserEmail));
  }

  @Test
  @DisplayName("test user logout updates global context")
  void logoutTestUser() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUser();
    testUser.login(GlobalContext.readFromFile());

    // run `terra auth revoke`
    TestCommand.Result cmd = TestCommand.runCommand("auth", "revoke");
    assertEquals(0, cmd.exitCode);

    // check that the credential store on disk is empty
    DataStore<StoredCredential> dataStore = TestUsers.getCredentialStore();
    assertEquals(0, dataStore.keySet().size(), "credential store is empty");

    // check that the current user in the global context != the test user
    // read the global context in from disk again to make sure it got persisted
    GlobalContext globalContext = GlobalContext.readFromFile();
    Optional<TerraUser> currentTerraUser = globalContext.getCurrentTerraUser();
    assertFalse(currentTerraUser.isPresent(), "current user unset in global context");
  }

  @Test
  @DisplayName("all test users enabled in SAM")
  void checkEnabled() throws IOException {
    // check that each test user is enabled in SAM
    for (TestUsers testUser : Arrays.asList(TestUsers.values())) {
      // login the user, so we have their credentials
      GlobalContext globalContext = GlobalContext.readFromFile();
      testUser.login(globalContext);

      // build a SAM client with the test user's credentials
      SamService samService =
          new SamService(globalContext.server, globalContext.requireCurrentTerraUser());

      // check that the user is enabled
      UserStatusInfo userStatusInfo = samService.getUserInfo();
      assertTrue(userStatusInfo.getEnabled(), "test user is enabled in SAM");

      // run `terra auth revoke`
      TestCommand.Result cmd = TestCommand.runCommand("auth", "revoke");
      assertEquals(0, cmd.exitCode);
    }
  }
}
