package bio.terra.cli.businessobject;

import bio.terra.cli.app.utils.AppDefaultCredentialUtils;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.PDUser;
import bio.terra.cli.service.GoogleOauth;
import bio.terra.cli.service.SamService;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.UserIO;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
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

  // pet SA email that Terra associates with this user. the CLI queries SAM to populate this field.
  private String petSAEmail;

  // Google credentials for the user. This can be either 1) end-user google credentials from an
  // oauth browser flow, or 2) end-user or pet sa google credentials pulled from the application
  // default credentials.
  private GoogleCredentials googleCredentials;

  /**
   * User specified to use app-default-credentials when logging in. In the follow-on command, we
   * check for ADC instead of user credentials that are stored on disk.
   */
  private boolean useApplicationDefaultCredentials;

  // these are the same scopes requested by Terra service swagger pages
  @VisibleForTesting
  public static final List<String> USER_SCOPES = ImmutableList.of("openid", "email", "profile");

  // these are the same scopes requested by Terra service swagger pages, plus the cloud platform
  // scope. pet SAs need the cloud platform scope to talk to GCP directly (e.g. to check the status
  // of a GCP notebook)
  private static final List<String> PET_SA_SCOPES =
      ImmutableList.of(
          "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform");

  // google OAuth client secret file
  // (https://developers.google.com/adwords/api/docs/guides/authentication#create_a_client_id_and_client_secret)
  private static final String CLIENT_SECRET_FILENAME = "client_secret.json";

  // URL of the landing page shown in the browser after completing the OAuth part of login
  // ideally this should point to product documentation or perhaps the UI, but for now the CLI
  // README seems like the best option. in the future, if we want to make this server-specific, then
  // it should become a property of the Server
  private static final String LOGIN_LANDING_PAGE =
      "https://github.com/DataBiosphere/terra-cli/blob/main/README.md";

  /** Build an instance of this class from the serialized format on disk. */
  public User(PDUser configFromDisk) {
    this.id = configFromDisk.id;
    this.email = configFromDisk.email;
    this.proxyGroupEmail = configFromDisk.proxyGroupEmail;
    this.petSAEmail = configFromDisk.petSAEmail;
    this.useApplicationDefaultCredentials = configFromDisk.useApplicationDefaultCredentials;
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
      if (useApplicationDefaultCredentials) {
        loadAppDefaultCredentials();
      } else {
        googleCredentials =
            GoogleOauth.getExistingUserCredential(
                USER_SCOPES, inputStream, Context.getContextDir().toFile());
      }
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
  }

  /**
   * Load any existing credentials for the user. Prompt for login if they are expired or do not
   * exist. Do not use application default credentials for logging in.
   */
  public static void login() {
    login(false);
  }

  /**
   * Load any existing credentials for this user. Prompt for login if they are expired or do not
   * exist.
   *
   * @param useAdc whether to use application default credentials to log in.
   */
  public static void login(boolean useAdc) {
    Optional<User> currentUser = Context.getUser();
    currentUser.ifPresent(User::loadExistingCredentials);

    // populate the current user object or build a new one
    User user = currentUser.orElseGet(() -> new User());

    if (useAdc) {
      user.loadAppDefaultCredentials();
    } else {
      if (currentUser.isEmpty() || currentUser.get().requiresReauthentication()) {
        user.doOauthLoginFlow();
      }
    }

    // if this is a new login...
    if (currentUser.isEmpty()) {
      try {
        user.fetchUserInfo();
      } catch (Exception exception) {
        user.deleteOauthCredentials();
        throw exception;
      }

      // update the global context on disk
      Context.setUser(user);

      // load the workspace metadata (if not already loaded), and the pet SA credentials
      if (Context.getWorkspace().isPresent()) {
        user.fetchWorkspaceInfo();
      }
    }
  }

  /**
   * Fetches a Google Application Credentials with {@code PET_SA_SCOPES} and set to {@code
   * googleCredentials}.
   */
  private void loadAppDefaultCredentials() {
    if (googleCredentials == null) {
      useApplicationDefaultCredentials = true;
      googleCredentials =
          AppDefaultCredentialUtils.getApplicationDefaultCredentials().createScoped(PET_SA_SCOPES);
    }
  }

  /** Delete all credentials associated with this user. */
  public void logout() {
    deleteOauthCredentials();
    deletePetSaCredentials();
    if (googleCredentials != null) {
      GoogleOauth.revokeToken(googleCredentials);
    }

    // unset the current user in the global context
    Context.setUser(null);
  }

  /**
   * Fetch the pet SA email for this user + current workspace from SAM and persist it in the global
   * context directory. If the pet SA does not yet exist for the user, then SAM will create it.
   * Grant both the user and the pet SA permission to impersonate the pet SA. This method assumes
   * that this user is the current user, and that there is a current workspace specified.
   */
  public void fetchPetSaCredentials() {
    // if the cloud context is undefined, then something went wrong during workspace creation
    // just log an error here instead of throwing an exception, so that the workspace load will
    // will succeed and the user can delete the corrupted workspace
    Workspace currentWorkspace = Context.requireWorkspace();
    String googleProjectId = currentWorkspace.getGoogleProjectId();
    if (googleProjectId == null || googleProjectId.isEmpty()) {
      logger.error("No Google context for the current workspace. Skip fetching pet SA from SAM.");
      return;
    }

    // ask SAM for the project-specific pet SA email and persist it on disk
    petSAEmail = SamService.forUser(this).getPetSaEmailForProject(googleProjectId);
    Context.setUser(this);

    // Allow the user and their pet to impersonate the pet service account so that Nextflow and
    // other app calls can run.
    // TODO(PF-991): This behavior will change in the future when WSM disallows SA
    //  self-impersonation
    currentWorkspace.enablePet();
    logger.debug("Enabled pet SA impersonation");
  }

  /**
   * Fetch the pet SA key file for this user + current workspace from SAM and persist it in the
   * global context directory. This method assumes that this user is the current user, and that
   * there is a current workspace specified.
   *
   * <p>This method should only used for testing, because the relevant SAM endpoint may not be
   * implemented in prod environments.
   *
   * @return the absolute path to the pet SA key file
   */
  @VisibleForTesting
  public Path fetchPetSaKeyFile() {
    // if the key file already exists on disk, just return it
    Path jsonKeyPath = Context.getPetSaKeyFile(this);
    if (jsonKeyPath.toFile().exists()) {
      return jsonKeyPath;
    }

    // ask SAM for the project-specific pet SA key
    HttpUtils.HttpResponse petSaKeySamResponse =
        SamService.forUser(this)
            .getPetSaKeyForProject(Context.requireWorkspace().getGoogleProjectId());
    logger.debug("SAM response to pet SA key request: {})", petSaKeySamResponse);
    if (!HttpStatusCodes.isSuccess(petSaKeySamResponse.statusCode)) {
      throw new SystemException(
          "Error fetching pet SA key from SAM (status code = "
              + petSaKeySamResponse.statusCode
              + ").");
    }
    try {
      // persist the key file in the global context directory
      FileUtils.writeStringToFile(jsonKeyPath.toFile(), petSaKeySamResponse.responseBody);
    } catch (IOException ioEx) {
      throw new SystemException("Error writing pet SA key to the global context directory.", ioEx);
    }
    logger.debug("Stored pet SA key file for this user and workspace: {}", jsonKeyPath);
    return jsonKeyPath;
  }

  /** Delete this user's OAuth credentials. */
  private void deleteOauthCredentials() {
    try (InputStream inputStream =
        User.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      GoogleOauth.deleteExistingCredential(
          USER_SCOPES, inputStream, Context.getContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error deleting credentials.", ex);
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

    this.petSAEmail = null;
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
      googleCredentials =
          GoogleOauth.doLoginAndConsent(
              USER_SCOPES,
              inputStream,
              Context.getContextDir().toFile(),
              launchBrowserAutomatically,
              LOGIN_LANDING_PAGE);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }
  }

  /** Fetch the user information (id, email, proxy group email) for this user from SAM. */
  private void fetchUserInfo() {
    SamService samService = SamService.forUser(this);
    UserStatusInfo userInfo = samService.getUserInfoOrRegisterUser();
    id = userInfo.getUserSubjectId();
    email = userInfo.getUserEmail();
    proxyGroupEmail = samService.getProxyGroupEmail(email);
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
    return petSAEmail;
  }

  public GoogleCredentials getGoogleCredentials() {
    return googleCredentials;
  }

  public GoogleCredentials getPetSACredentials() {
    return GoogleCredentials.create(getPetSaAccessToken());
  }

  public boolean isUseApplicationDefaultCredentials() {
    return useApplicationDefaultCredentials;
  }

  /** Return true if the user credentials are expired or do not exist on disk. */
  public boolean requiresReauthentication() {
    if (googleCredentials == null) {
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
    return GoogleOauth.getAccessToken(googleCredentials);
  }

  /** Get the access token for the pet SA credentials. */
  public AccessToken getPetSaAccessToken() {
    String googleProjectId = Context.requireWorkspace().getGoogleProjectId();
    String accessTokenStr =
        SamService.forUser(this).getPetSaAccessTokenForProject(googleProjectId, PET_SA_SCOPES);
    return new AccessToken(accessTokenStr, null);
  }
}
