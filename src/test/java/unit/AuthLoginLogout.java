package unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.service.GoogleOauth;
import bio.terra.cli.service.SamService;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.auth.oauth2.IdToken;
import harness.TestCommand;
import harness.TestUser;
import harness.baseclasses.ClearContextUnit;
import java.io.IOException;
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
public class AuthLoginLogout extends ClearContextUnit {
  @Test
  @DisplayName("test user login updates global context")
  void loginTestUser() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // check that the credential exists in the store on disk
    DataStore<StoredCredential> credentialStore = TestUser.getCredentialStore();
    assertThat("credential store contains two entries", credentialStore.keySet(), hasSize(2));
    assertTrue(
        credentialStore.containsKey(GoogleOauth.CREDENTIAL_STORE_KEY),
        "credential store contains hard-coded user key");
    assertTrue(
        credentialStore.containsKey(GoogleOauth.ID_TOKEN_STORE_KEY),
        "credential store contains  id token");
    StoredCredential storedCredential = credentialStore.get(GoogleOauth.CREDENTIAL_STORE_KEY);
    assertThat(storedCredential.getAccessToken(), CoreMatchers.not(emptyOrNullString()));

    DataStore<IdToken> idTokenStore = TestUser.getCredentialStore();
    assertEquals(
        idTokenStore.keySet(),
        credentialStore.keySet(),
        "credentialStore and idTokenStore are different views of same underlying datastore");
    assertThat(
        idTokenStore.get(GoogleOauth.ID_TOKEN_STORE_KEY).getTokenValue(),
        CoreMatchers.not(emptyOrNullString()));

    // check that the current user in the global context = the test user
    Optional<User> currentUser = Context.getUser();
    assertTrue(currentUser.isPresent(), "current user set in global context");
    assertThat(
        "test user email matches the current user set in global context",
        testUser.email,
        equalToIgnoringCase(currentUser.get().getEmail()));
  }

  @Test
  @DisplayName("test user logout updates global context")
  void logoutTestUser() throws IOException {
    // select a test user and login
    TestUser testUser = TestUser.chooseTestUser();
    testUser.login();

    // `terra auth revoke`
    TestCommand.runCommandExpectSuccess("auth", "revoke");

    // check that the credential store on disk is empty
    DataStore<StoredCredential> dataStore = TestUser.getCredentialStore();
    assertEquals(0, dataStore.keySet().size(), "credential store is empty");

    // read the global context in from disk again to check what got persisted
    // check that the current user in the global context is unset
    Optional<User> currentUser = Context.getUser();
    assertFalse(currentUser.isPresent(), "current user unset in global context");
  }

  @Test
  @DisplayName("all test users enabled in SAM")
  void checkEnabled() throws IOException {
    // check that each test user is enabled in SAM
    for (TestUser testUser : TestUser.getTestUsers()) {
      // login the user, so we have their credentials
      testUser.login();

      // check that the user is enabled
      UserStatusInfo userStatusInfo = SamService.fromContext().getUserInfoForSelf();
      assertTrue(userStatusInfo.getEnabled(), "test user is enabled in SAM");

      // `terra auth revoke`
      TestCommand.runCommandExpectSuccess("auth", "revoke");
    }
  }
}
