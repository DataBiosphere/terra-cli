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
import java.util.UUID;
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

  public AuthManager(GlobalContext globalContext) {
    this.globalContext = globalContext;
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
    TerraUser userLoggingIn = globalContext.getCurrentTerraUser();
    if (userLoggingIn == null) {
      // generate a random string to use as the CLI user identifer
      // it would be better to use the SAM subject id here instead, but we can't talk to SAM until
      // we have Google (user) credentials, and we need to give the Google login flow an identifer
      // for storing the credential in a persistent file. so, this is the id that we hand to Google
      // and can use later to look up the persisted Google (user) credentials
      String cliGeneratedUserKey = UUID.randomUUID().toString();
      userLoggingIn = new TerraUser(cliGeneratedUserKey);
    }

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      userCredentials =
          AuthenticationUtils.doLoginAndConsent(
              userLoggingIn.cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    userLoggingIn.userCredentials = userCredentials;

    // fetch the user information from SAM (subject id = terra user id, email = terra user name)
    SAMUtils.populateTerraUserInfo(userLoggingIn, globalContext);

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
              userLoggingIn.terraUserId,
              petSAKeySAMResponse.responseBody);

      // create a credentials object from the key
      ServiceAccountCredentials petSACredentials =
          AuthenticationUtils.getServiceAccountCredential(jsonKeyFile.toFile(), SCOPES);

      userLoggingIn.petSACredentials = petSACredentials;
    } catch (IOException ioEx) {
      logger.error("Error writing pet SA key to the global context directory.", ioEx);
    }

    // update the global context with the current user
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    globalContext.addOrUpdateTerraUser(userLoggingIn);
    globalContext.setCurrentTerraUser(userLoggingIn);
    globalContext.writeToFile();
  }

  /** Delete all credentials associated with the current Terra user. */
  public void logoutTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }
    if (globalContext.getCurrentTerraUser() == null) {
      logger.debug("There is no current Terra user defined in the global context.");
      return;
    }

    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      AuthenticationUtils.deleteExistingCredential(
          globalContext.getCurrentTerraUser().cliGeneratedUserKey,
          SCOPES,
          inputStream,
          globalContext.resolveGlobalContextDir().toFile());

      // delete the pet SA credentials
      File jsonKeyFile =
          globalContext
              .resolvePetSAKeyDir()
              .resolve(globalContext.getCurrentTerraUser().terraUserId)
              .toFile();
      if (!jsonKeyFile.delete()) {
        throw new RuntimeException("Failed to delete pet SA key file.");
      }
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error deleting credentials.", ex);
    }
  }

  /**
   * Fetch the current Terra user, as defined in the global context, along with any existing
   * credentials. Returns null if there is no current Terra user defined.
   */
  public void populateCurrentTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }
    if (globalContext.getCurrentTerraUser() == null) {
      logger.debug("There is no current Terra user defined in the global context.");
      return;
    }

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // if there are no valid credentials for this user, then this method will return null
      userCredentials =
          AuthenticationUtils.getExistingUserCredential(
              globalContext.getCurrentTerraUser().cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    globalContext.getCurrentTerraUser().userCredentials = userCredentials;

    // if there are existing user credentials, then try to fetch the existing pet SA credentials
    // also
    if (userCredentials != null) {
      File jsonKeyFile =
          globalContext
              .resolvePetSAKeyDir()
              .resolve(globalContext.getCurrentTerraUser().terraUserId)
              .toFile();
      if (!jsonKeyFile.exists() || !jsonKeyFile.isFile()) {
        throw new RuntimeException("Pet SA key file not found.");
      }
      try {
        ServiceAccountCredentials petSACredentials =
            AuthenticationUtils.getServiceAccountCredential(jsonKeyFile, SCOPES);
        globalContext.getCurrentTerraUser().petSACredentials = petSACredentials;
      } catch (IOException ioEx) {
        throw new RuntimeException("Error reading pet SA key file.", ioEx);
      }
    }
  }
}
