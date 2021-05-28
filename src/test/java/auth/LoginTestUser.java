package auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.cli.auth.GoogleCredentialUtils;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.utils.SamService;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import harness.TestContext;
import harness.TestUsers;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class LoginTestUser {
  @BeforeEach
  void setup() throws IOException {
    TestContext.resetGlobalContext();
  }

  @AfterEach
  void cleanup() throws IOException {
    TestContext.deleteGlobalContext();
  }

  @Test
  @DisplayName("test user login updates global context")
  void loginTestUser() throws IOException {
    // select a test user and login
    TestUsers testUser = TestUsers.chooseTestUser();
    testUser.login(GlobalContext.get());

    // check that the credential exists in the store on disk
    DataStore<StoredCredential> dataStore = TestUsers.getCredentialStore();
    assertEquals(1, dataStore.keySet().size(), "credential store only contains one entry");
    assertTrue(
        dataStore.containsKey(GoogleCredentialUtils.CREDENTIAL_STORE_KEY),
        "credential store contains hard-coded single user key");
    StoredCredential storedCredential = dataStore.get(GoogleCredentialUtils.CREDENTIAL_STORE_KEY);
    assertThat(storedCredential.getAccessToken(), CoreMatchers.not(emptyOrNullString()));

    // check that the current user in the global context = the test user
    Optional<TerraUser> currentTerraUser = GlobalContext.get().getCurrentTerraUser();
    assertTrue(currentTerraUser.isPresent(), "current user set in global context");
    assertThat(
        "test user email matches the current user set in global context",
        testUser.email,
        equalToIgnoringCase(currentTerraUser.get().terraUserEmail));
  }

  @Test
  @DisplayName("all test users enabled in SAM")
  void checkEnabled() throws IOException {
    // check that each test user is enabled in SAM
    for (TestUsers testUser : Arrays.asList(TestUsers.values())) {
      // login the user, so we have their credentials
      GlobalContext globalContext = GlobalContext.get();
      testUser.login(globalContext);

      // build a SAM client with the test user's credentials
      SamService samService =
          new SamService(globalContext.server, globalContext.requireCurrentTerraUser());

      // check that the user is enabled
      UserStatusInfo userStatusInfo = samService.getUserInfo();
      assertTrue(userStatusInfo.getEnabled(), "test user is enabled in SAM");

      globalContext.requireCurrentTerraUser().logout();
    }
  }
}
