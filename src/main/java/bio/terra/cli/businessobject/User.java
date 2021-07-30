package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.PDUser;
import bio.terra.cli.service.GoogleOauth;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.UserIO;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
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

  // these are the same scopes requested by Terra service swagger pages
  @VisibleForTesting
  public static final List<String> USER_SCOPES = ImmutableList.of("openid", "email", "profile");

  // these are the same scopes requested by Terra service swagger pages, plus the cloud platform
  // scope. pet SAs need the cloud platform scope to talk to GCP directly (e.g. to check the status
  // of an AI notebook)
  private static final List<String> PET_SA_SCOPES =
      ImmutableList.of(
          "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform");

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
   * Load any existing credentials for this user. Return silently, do not prompt for login, if they
   * are expired or do not exist on disk.
   */
  public void loadExistingCredentials() {
    // load existing user credentials from disk
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {
      userCredentials =
          GoogleOauth.getExistingUserCredential(
              USER_SCOPES, inputStream, Context.getContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }

    // load existing pet SA credentials from disk
    if (Context.getWorkspace().isEmpty() || !Context.requireWorkspace().getIsLoaded()) {
      logger.debug(
          "Current workspace is either not defined or not loaded, so there are no pet SA credentials.");
      return;
    }
    Path jsonKeyPath = Context.getPetSaKeyFile(this);
    logger.debug("Looking for pet SA key file at: {}", jsonKeyPath);
    if (!jsonKeyPath.toFile().exists()) {
      logger.debug("Pet SA key file not found for current user + workspace");
      return;
    }
    petSACredentials = createSaCredentials(jsonKeyPath);
  }

  /**
   * Load any existing credentials for this user. Prompt for login if they are expired or do not
   * exist.
   */
  public static void login() {
    Optional<User> currentUser = Context.getUser();
    currentUser.ifPresent(User::loadExistingCredentials);

    // populate the current user object or build a new one
    User user = currentUser.orElseGet(() -> new User());

    // do the login flow if the current user is undefined or has expired credentials
    if (currentUser.isEmpty() || currentUser.get().requiresReauthentication()) {
      user.doOauthLoginFlow();
    }

    // if this is a new login...
    if (currentUser.isEmpty()) {
      user.fetchUserInfo();

      // update the global context on disk
      Context.setUser(user);

      // load the workspace metadata (if not already loaded), and the pet SA credentials
      if (Context.getWorkspace().isPresent()) {
        user.fetchWorkspaceInfo();
      }
    }
  }

  /** Delete all credentials associated with this user. */
  public void logout() {
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      GoogleOauth.deleteExistingCredential(
          USER_SCOPES, inputStream, Context.getContextDir().toFile());

      // delete the pet SA credentials
      deletePetSaCredentials();

      // unset the current user in the global context
      Context.setUser(null);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error deleting credentials.", ex);
    }
  }

  /**
   * Fetch the pet SA credentials for this user + current workspace from SAM and persist it in the
   * global context directory.
   */
  public void fetchPetSaCredentials() {
    // if the cloud context is undefined, then something went wrong during workspace creation
    // just log an error here so that login will succeed, and the user can go back and delete the
    // corrupted workspace
    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();
    if (googleProjectId == null) {
      logger.error(
          "No Google context for the current workspace. Skip fetching pet SA from SAM. MARIKO");
      return;
    }

    // ask SAM for the project-specific pet SA key
    HttpUtils.HttpResponse petSaKeySamResponse =
        new SamService(this, Context.getServer()).getPetSaKeyForProject(googleProjectId);
    logger.debug("SAM response to pet SA key request: {})", petSaKeySamResponse);
    if (!HttpStatusCodes.isSuccess(petSaKeySamResponse.statusCode)) {
      throw new SystemException(
          "Error fetching pet SA key from SAM (status code = "
              + petSaKeySamResponse.statusCode
              + ").");
    }
    Path jsonKeyPath;
    try {
      // persist the key file in the global context directory
      jsonKeyPath =
          FileUtils.writeStringToFile(
              Context.getPetSaKeyFile(this).toFile(), petSaKeySamResponse.responseBody);
    } catch (IOException ioEx) {
      throw new SystemException("Error writing pet SA key to the global context directory.", ioEx);
    }
    logger.debug("Stored pet SA key file for this user and workspace.");
    petSACredentials = createSaCredentials(jsonKeyPath);
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

  /**
   * Do the OAuth login flow. If there is an existing non-expired credential stored on disk, then we
   * load that. If not, then we prompt the user for the requested user scopes.
   */
  private void doOauthLoginFlow() {
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // log the user in and get their consent to the requested scopes
      boolean launchBrowserAutomatically =
          Context.getConfig().getBrowserLaunchOption().equals(Config.BrowserLaunchOption.AUTO);
      userCredentials =
          GoogleOauth.doLoginAndConsent(
              USER_SCOPES,
              inputStream,
              Context.getContextDir().toFile(),
              launchBrowserAutomatically);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }
  }

  /** Fetch the user information (id, email, proxy group email) for this user from SAM. */
  private void fetchUserInfo() {
    SamService samService = new SamService(this, Context.getServer());
    UserStatusInfo userInfo = samService.getUserInfoOrRegisterUser();
    id = userInfo.getUserSubjectId();
    email = userInfo.getUserEmail();
    proxyGroupEmail = samService.getProxyGroupEmail();
  }

  /**
   * Fetch the workspace metadata (if it's not already loaded) and the pet SA credentials. Don't
   * throw an exception if it fails, because that shouldn't block a successful login, but do log the
   * error to the console.
   */
  private void fetchWorkspaceInfo() {
    try {
      if (!Context.requireWorkspace().getIsLoaded()) {
        // if the workspace was set without credentials, load the workspace metadata and pet SA
        Workspace.load(Context.requireWorkspace().getId());
      } else {
        // otherwise, just load the pet SA
        fetchPetSaCredentials();
      }
    } catch (Exception ex) {
      logger.error("Error loading workspace or pet SA credentials during login", ex);
      UserIO.getErr()
          .println(
              "Error loading workspace information for the logged in user (workspace id: "
                  + Context.requireWorkspace().getId()
                  + ").");
    }
  }

  /** Create a credentials object for a service account from a key file. */
  private static ServiceAccountCredentials createSaCredentials(Path jsonKeyPath) {
    try {
      return GoogleOauth.getServiceAccountCredential(jsonKeyPath.toFile(), PET_SA_SCOPES);
    } catch (IOException ioEx) {
      throw new SystemException("Error reading SA key file.", ioEx);
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

  public String getPetSaEmail() {
    return petSACredentials == null ? null : petSACredentials.getClientEmail();
  }

  public UserCredentials getUserCredentials() {
    return userCredentials;
  }

  public ServiceAccountCredentials getPetSACredentials() {
    return petSACredentials;
  }

  /** Return true if the user credentials are expired or do not exist on disk. */
  public boolean requiresReauthentication() {
    if (userCredentials == null) {
      return true;
    }

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
}
