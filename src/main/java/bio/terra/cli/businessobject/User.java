package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.PDUser;
import bio.terra.cli.service.GoogleOauth;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.FileUtils;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a user. An instance of this class is part of the current context or
 * state.
 */
@SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification =
        "Known false positive for certain try-with-resources blocks, which are used in several methods in this class. https://github.com/spotbugs/spotbugs/issues/1338 (Other similar issues linked from there.)")
public class User {
  private static final Logger logger = LoggerFactory.getLogger(User.class);

  // id that Terra uses to identify a user. the CLI queries SAM for a user's subject id to populate
  // this field.
  private String id;

  // name that Terra associates with this user. the CLI queries SAM for a user's email to populate
  // this field.
  private String email;

  // proxy group email that Terra associates with this user. permissions granted to the proxy group
  // are transitively granted to the user and all of their pet SAs. the CLI queries SAM to populate
  // this field.
  private String proxyGroupEmail;

  // Google credentials for the user and their pet SA
  private UserCredentials userCredentials;
  private ServiceAccountCredentials petSACredentials;

  @VisibleForTesting
  public static final List<String> SCOPES =
      Collections.unmodifiableList(
          Arrays.asList(
              "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform"));

  // google OAuth client secret file
  // (https://developers.google.com/adwords/api/docs/guides/authentication#create_a_client_id_and_client_secret)
  private static final String CLIENT_SECRET_FILENAME = "client_secret.json";

  /** Build an instance of this class from the serialized format on disk. */
  public User(PDUser configFromDisk) {
    this.id = configFromDisk.id;
    this.email = configFromDisk.email;
    this.proxyGroupEmail = configFromDisk.proxyGroupEmail;
  }

  /** Build an empty instance of this class. */
  private User() {}

  /**
   * Fetch all credentials for a new user, and set them as the current Terra user. Prompt for login
   * and consent if they do not already exist or are expired.
   */
  public static void login() {
    Optional<User> currentUser = Context.getUser();

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      boolean launchBrowserAutomatically =
          Context.getConfig().getBrowserLaunchOption().equals(Config.BrowserLaunchOption.AUTO);
      userCredentials =
          GoogleOauth.doLoginAndConsent(
              SCOPES, inputStream, Context.getContextDir().toFile(), launchBrowserAutomatically);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }

    // build or populate the current terra user object
    User user = currentUser.orElseGet(() -> new User());
    user.userCredentials = userCredentials;

    // fetch the user information from SAM, if it's not already populated
    if (!currentUser.isPresent()) {
      SamService samService = new SamService(user, Context.getServer());
      UserStatusInfo userInfo = samService.getUserInfoOrRegisterUser();
      user.id = userInfo.getUserSubjectId();
      user.email = userInfo.getUserEmail();
      user.proxyGroupEmail = samService.getProxyGroupEmail();
    }

    // update the global context on disk with the current user
    if (!currentUser.isPresent()) {
      Context.setUser(user);
    }

    // always fetch the pet SA credentials because there is a different pet SA per workspace
    // do this after updating the context so that we can call Context.requireUser in this method
    user.fetchPetSaCredentials();
  }

  /** Delete all credentials associated with this user. */
  public void logout() {
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      GoogleOauth.deleteExistingCredential(SCOPES, inputStream, Context.getContextDir().toFile());

      // delete the pet SA credentials
      deletePetSaCredentials();

      // unset the current user in the global context
      Context.setUser(null);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error deleting credentials.", ex);
    }
  }

  /** Check if the user credentials are expired. */
  public boolean requiresReauthentication() {
    if (userCredentials == null) {
      // fetch existing user credentials
      try (InputStream inputStream =
          User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {
        userCredentials =
            GoogleOauth.getExistingUserCredential(
                SCOPES, inputStream, Context.getContextDir().toFile());
        if (userCredentials == null) {
          return true;
        }
      } catch (IOException | GeneralSecurityException ex) {
        throw new SystemException("Error fetching user credentials.", ex);
      }
    }

    // fetch the user access token
    // this method call will attempt to refresh the token if it's already expired
    AccessToken accessToken = getUserAccessToken();

    // check if the token is expired
    logger.debug("Access token expiration date: {}", accessToken.getExpirationTime());
    return accessToken.getExpirationTime().compareTo(new Date()) <= 0;
  }

  /** Get the access token for the user credentials. */
  public AccessToken getUserAccessToken() {
    return GoogleOauth.getAccessToken(userCredentials);
  }

  /** Get the access token for the pet SA credentials. */
  public AccessToken getPetSaAccessToken() {
    return GoogleOauth.getAccessToken(petSACredentials);
  }

  /**
   * Get the client email from the pet SA key file. Returns null if there is no current workspace
   * defined.
   */
  public String getPetSaEmail() {
    // pet SAs are workspace-specific. if the current workspace is not defined, there is no pet SA
    // to fetch
    if (Context.getWorkspace().isEmpty()) {
      logger.debug("No current workspace defined, so there are no pet SA credentials.");
      return null;
    }
    Path jsonKeyPath = Context.getPetSaKeyFile();
    logger.debug("Looking for pet SA key file at: {}", jsonKeyPath);
    if (!jsonKeyPath.toFile().exists()) {
      throw new SystemException("Pet SA key file not found for current user + workspace");
    }
    // create a credentials object from the key
    try {
      petSACredentials = GoogleOauth.getServiceAccountCredential(jsonKeyPath.toFile(), SCOPES);
    } catch (IOException ioEx) {
      throw new SystemException("Error reading pet SA key file.", ioEx);
    }
    return petSACredentials.getClientEmail();
  }

  /** Fetch the pet SA credentials for this user + current workspace. */
  public void fetchPetSaCredentials() {
    // pet SAs are workspace-specific. if the current workspace is not defined, there is no pet SA
    // to fetch
    if (Context.getWorkspace().isEmpty()) {
      logger.debug("No current workspace defined. Skipping fetch of pet SA credentials.");
      return;
    }

    // if the key file for this user + workspace already exists, then no need to re-fetch
    Path jsonKeyPath = Context.getPetSaKeyFile();
    logger.debug("Looking for pet SA key file at: {}", jsonKeyPath);
    if (jsonKeyPath.toFile().exists()) {
      logger.debug("Pet SA key file for this user and workspace already exists.");
    } else {
      // ask SAM for the project-specific pet SA key
      HttpUtils.HttpResponse petSaKeySamResponse =
          new SamService(this, Context.getServer())
              .getPetSaKeyForProject(Context.requireWorkspace().getGoogleProjectId());
      if (!HttpStatusCodes.isSuccess(petSaKeySamResponse.statusCode)) {
        logger.debug("SAM response to pet SA key request: {})", petSaKeySamResponse.responseBody);
        throw new SystemException(
            "Error fetching pet SA key from SAM (status code = "
                + petSaKeySamResponse.statusCode
                + ").");
      }
      try {
        // persist the key file in the global context directory
        jsonKeyPath =
            FileUtils.writeStringToFile(
                Context.getPetSaKeyFile().toFile(), petSaKeySamResponse.responseBody);
        logger.debug("Stored pet SA key file for this user and workspace.");
      } catch (IOException ioEx) {
        throw new SystemException(
            "Error writing pet SA key to the global context directory.", ioEx);
      }
    }

    try {
      // create a credentials object from the key
      petSACredentials = GoogleOauth.getServiceAccountCredential(jsonKeyPath.toFile(), SCOPES);
    } catch (IOException ioEx) {
      throw new SystemException(
          "Error reading pet SA credentials from the global context directory.", ioEx);
    }
  }

  /** Delete all pet SA credentials for this user. */
  public void deletePetSaCredentials() {
    File jsonKeysDir = Context.getPetSaKeyDir(this).toFile();

    // delete all key files
    File[] keyFiles = jsonKeysDir.listFiles();
    if (keyFiles != null) {
      for (File keyFile : keyFiles) {
        if (!keyFile.delete() && keyFile.exists()) {
          throw new SystemException(
              "Failed to delete pet SA key file: " + keyFile.getAbsolutePath());
        }
      }
    }

    // delete the key file directory
    if (!jsonKeysDir.delete() && jsonKeysDir.exists()) {
      throw new SystemException(
          "Failed to delete pet SA key file sub-directory: " + jsonKeysDir.getAbsolutePath());
    }
  }

  // ====================================================
  // Property getters.
  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getProxyGroupEmail() {
    return proxyGroupEmail;
  }

  public UserCredentials getUserCredentials() {
    return userCredentials;
  }

  public ServiceAccountCredentials getPetSACredentials() {
    return petSACredentials;
  }
}
