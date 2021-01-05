package bio.terra.cli.auth;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.utils.AuthenticationUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.io.InputStream;
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

  private static final String CLIENT_SECRET_FILENAME = "client_secret.json";

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

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    GoogleCredentials userCredentials;
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

    // fetch the pet SA credentials. Prompt for login and consent if they do not already exist or
    // are expired.
    ServiceAccountCredentials petSACredentials = null;
    // check if already exist
    // call SAM to fetch credentials
    // persist them in .terra-cli

    // fetch the profile name
    String terraUserName = "[USERNAME]";

    // update the state of AuthManager to include the credentials for this current Terra user
    // note that this state is not persisted to disk. it will be useful only for code called in the
    // same CLI command/process
    currentTerraUser =
        new TerraUser(
                globalContext
                    .getSingleUserId()) // TODO: replace this with the unique google account id
            .terraUserName(terraUserName)
            .userCredentials(userCredentials)
            .petSACredentials(petSACredentials);

    // update the global context with the current user
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    globalContext.setCurrentTerraUserId(currentTerraUser.getTerraUserId());
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
      // call AuthUtils.deleteExistingCredential with the SA id
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
  public TerraUser populateCurrentTerraUser() {
    if (globalContext == null) {
      throw new RuntimeException("Error reading global context.");
    }

    String currentTerraUserId = globalContext.getCurrentTerraUserId();
    if (currentTerraUserId == null) {
      currentTerraUser = null;
      logger.debug("There is no current Terra user defined in the global context.");
      return currentTerraUser;
    }

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    GoogleCredentials userCredentials;
    try (InputStream inputStream =
        AuthManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      userCredentials =
          AuthenticationUtils.getExistingUserCredential(
              globalContext.getSingleUserId(),
              SCOPES,
              inputStream,
              globalContext.getGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }

    // fetch the pet SA credentials also
    ServiceAccountCredentials petSACredentials = null;

    // fetch the profile name
    String terraUserName = "[USERNAME]";

    // update the state of AuthManager to include the credentials for this current Terra user
    // note that this state is not persisted to disk. it will be useful only for code called in the
    // same CLI command/process
    currentTerraUser =
        new TerraUser(
                globalContext
                    .getSingleUserId()) // TODO: replace this with the unique google account id
            .terraUserName(terraUserName)
            .userCredentials(userCredentials)
            .petSACredentials(petSACredentials);

    return currentTerraUser;
  }

  /** Getter for the current Terra user property. */
  public TerraUser getCurrentTerraUser() {
    return currentTerraUser;
  }
}
