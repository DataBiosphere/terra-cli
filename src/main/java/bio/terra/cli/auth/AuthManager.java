package bio.terra.cli.auth;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.utils.AuthenticationUtils;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.HTTPUtils;
import bio.terra.cli.utils.SAMUtils;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthManager {
  private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);

  public static final List<String> SCOPES =
      Collections.unmodifiableList(
          Arrays.asList(
              "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform"));

  private static final String CLIENT_SECRET_FILENAME = "jadecli_client_secret.json";

  private final GlobalContext globalContext;
  private TerraUser currentTerraUser;

  private AuthManager(GlobalContext globalContext) {
    this.globalContext = globalContext;
  }

  public static AuthManager buildAuthManagerFromGlobalContext() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    return new AuthManager((globalContext));
  }

  /**
   * Fetch all credentials for a new user, and set them as the current Terra user. Prompt for login
   * and consent if they do not already exist or are expired.
   */
  public void loginTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }

    // this will become the current Terra user if we are successful in getting all the various
    // information and credentials below
    TerraUser userLoggingIn = new TerraUser();

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      userCredentials =
          AuthenticationUtils.doLoginAndConsent(
              globalContext.getSingleUserId(),
              SCOPES,
              inputStream,
              globalContext.getGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    userLoggingIn.userCredentials(userCredentials);

    // fetch the pet SA credentials from SAM
    HTTPUtils.HTTPResponse petSAKeySAMResponse = SAMUtils.getPetSAKey(userLoggingIn, globalContext);
    if (petSAKeySAMResponse.statusCode != 200) {
      throw new RuntimeException("Error fetching pet SA key from SAM");
    }
    try {
      // persist it in the global context directory
      Path jsonKeyFile =
          FileUtils.writeStringToFile(
              globalContext.resolvePetSAKeyDir(),
              globalContext.getPetSAId(),
              petSAKeySAMResponse.responseBody);

      // create a credentials object from the key
      ServiceAccountCredentials petSACredentials =
          AuthenticationUtils.getServiceAccountCredential(jsonKeyFile.toFile(), SCOPES);

      userLoggingIn.petSACredentials(petSACredentials);
    } catch (IOException ioEx) {
      logger.error("Error writing pet SA key to the global context directory.", ioEx);
    }

    // fetch the user information from SAM (subject id = terra user id, email = terra user name)
    SAMUtils.populateTerraUserInfo(userLoggingIn, globalContext);

    // update the state of AuthManager to include the credentials for this current Terra user
    // note that this state is not persisted to disk. it will be useful only for code called in the
    // same CLI command/process
    currentTerraUser = userLoggingIn;

    // update the global context with the current user
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    globalContext.setCurrentTerraUserId(currentTerraUser.getTerraUserId());
    globalContext.setCurrentTerraUserName(currentTerraUser.getTerraUserName());
    globalContext.writeToFile();
  }

  /** Delete all credentials associated with the current Terra user. */
  public void logoutTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }

    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      AuthenticationUtils.deleteExistingCredential(
          globalContext.getSingleUserId(),
          SCOPES,
          inputStream,
          globalContext.getGlobalContextDir().toFile());

      // delete the pet SA credentials
      File jsonKeyFile =
          globalContext.resolvePetSAKeyDir().resolve(globalContext.getPetSAId()).toFile();
      if (!jsonKeyFile.delete()) {
        throw new RuntimeException("Failed to delete pet SA key file.");
      }
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error deleting credentials.", ex);
    }

    // update the state of AuthManager to delete the credentials for the current Terra user
    // note that this state is not persisted to disk. it will be useful only for code called in the
    // same CLI command/process
    currentTerraUser = null;
  }

  /**
   * Fetch the current Terra user, as defined in the global context, along with any existing
   * credentials. Returns null if there is no current Terra user defined.
   */
  public void populateCurrentTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }

    String currentTerraUserId = globalContext.getCurrentTerraUserId();
    if (currentTerraUserId == null) {
      currentTerraUser = null;
      logger.debug("There is no current Terra user defined in the global context.");
      return;
    }

    // this will become the current Terra user if we are successful in getting all the various
    // information and credentials below
    TerraUser userAlreadyLoggedIn =
        new TerraUser()
            .terraUserId(currentTerraUserId)
            .terraUserName(globalContext.getCurrentTerraUserName());

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // if there are no valid credentials for this user, then this method will return null
      userCredentials =
          AuthenticationUtils.getExistingUserCredential(
              globalContext.getSingleUserId(),
              SCOPES,
              inputStream,
              globalContext.getGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    userAlreadyLoggedIn.userCredentials(userCredentials);

    // if there are existing user credentials, then try to fetch the existing pet SA credentials
    // also
    if (userCredentials != null) {
      File jsonKeyFile =
          globalContext.resolvePetSAKeyDir().resolve(globalContext.getPetSAId()).toFile();
      if (!jsonKeyFile.exists() || !jsonKeyFile.isFile()) {
        throw new RuntimeException("Pet SA key file not found.");
      }
      try {
        ServiceAccountCredentials petSACredentials =
            AuthenticationUtils.getServiceAccountCredential(jsonKeyFile, SCOPES);
        userAlreadyLoggedIn.petSACredentials(petSACredentials);
      } catch (IOException ioEx) {
        throw new RuntimeException("Error reading pet SA key file.", ioEx);
      }
    }

    // update the state of AuthManager to include the credentials for this current Terra user
    // note that this state is not persisted to disk. it will be useful only for code called in the
    // same CLI command/process
    currentTerraUser = userAlreadyLoggedIn;
  }

  /** Getter for the current Terra user property. */
  public TerraUser getCurrentTerraUser() {
    return currentTerraUser;
  }
}
