package bio.terra.cli.context;

import bio.terra.cli.auth.GoogleCredentialUtils;
import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.HttpUtils;
import bio.terra.cli.service.SamService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * This POJO class represents a Terra identity context, which includes all related credentials (e.g.
 * user, pet SA).
 */
@SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification =
        "Known false positive for certain try-with-resources blocks, which are used in several methods in this class. https://github.com/spotbugs/spotbugs/issues/1338 (Other similar issues linked from there.)")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TerraUser {
  private static final Logger logger = LoggerFactory.getLogger(TerraUser.class);

  // This field stores the id that Terra uses to identify a user. The CLI queries SAM for a user's
  // subject id to populate this field.
  private String terraUserId;

  // This field stores the name that Terra associates with this user. The CLI queries SAM for a
  // user's email to populate this field.
  private String terraUserEmail;

  // This field stores the proxy group email that Terra associates with this user. Permissions
  // granted to the proxy group are transitively granted to the user and all of their pet SAs. The
  // CLI queries SAM to populate this field.
  private String terraProxyGroupEmail;

  @JsonIgnore public UserCredentials userCredentials;
  @JsonIgnore public ServiceAccountCredentials petSACredentials;

  @VisibleForTesting
  public static final List<String> SCOPES =
      Collections.unmodifiableList(
          Arrays.asList(
              "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform"));

  private static final String CLIENT_SECRET_FILENAME = "client_secret.json";

  public TerraUser() {}

  /**
   * Fetch all credentials for a new user, and set them as the current Terra user. Prompt for login
   * and consent if they do not already exist or are expired.
   */
  public static void login() {
    GlobalContext globalContext = GlobalContext.get();
    Optional<TerraUser> currentTerraUser = globalContext.getCurrentTerraUser();

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        TerraUser.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      boolean launchBrowserAutomatically =
          globalContext.getBrowserLaunchOption().equals(GlobalContext.BrowserLaunchOption.auto);
      userCredentials =
          GoogleCredentialUtils.doLoginAndConsent(
              SCOPES,
              inputStream,
              GlobalContext.getGlobalContextDir().toFile(),
              launchBrowserAutomatically);
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }

    // build or populate the current terra user object
    TerraUser terraUser = currentTerraUser.orElseGet(() -> new TerraUser());
    terraUser.userCredentials = userCredentials;

    // fetch the user information from SAM, if it's not already populated
    if (!currentTerraUser.isPresent()) {
      SamService samService = new SamService(globalContext.getServer(), terraUser);
      UserStatusInfo userInfo = samService.getUserInfoOrRegisterUser();
      terraUser.terraUserId = userInfo.getUserSubjectId();
      terraUser.terraUserEmail = userInfo.getUserEmail();
      terraUser.terraProxyGroupEmail = samService.getProxyGroupEmail();
    }

    // always fetch the pet SA credentials because there is a different pet SA per workspace
    terraUser.fetchPetSaCredentials();

    // update the global context on disk with the current user
    globalContext.setCurrentTerraUser(terraUser);
  }

  /** Delete all credentials associated with this user. */
  public void logout() {
    try (InputStream inputStream =
        TerraUser.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      GoogleCredentialUtils.deleteExistingCredential(
          SCOPES, inputStream, GlobalContext.getGlobalContextDir().toFile());

      // delete the pet SA credentials
      deletePetSaCredentials();

      // unset the current user in the global context
      GlobalContext.get().unsetCurrentTerraUser();
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error deleting credentials.", ex);
    }
  }

  /** Check if the user credentials are expired. */
  public boolean requiresReauthentication() {
    // fetch existing user credentials
    try (InputStream inputStream =
        TerraUser.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {
      userCredentials =
          GoogleCredentialUtils.getExistingUserCredential(
              SCOPES, inputStream, GlobalContext.getGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new SystemException("Error fetching user credentials.", ex);
    }
    if (userCredentials == null) {
      return true;
    }

    // fetch the user access token
    // this method call will attempt to refresh the token if it's already expired
    AccessToken accessToken = getUserAccessToken();

    // check if the token is expired
    logger.debug("Access token expiration date: {}", accessToken.getExpirationTime());
    return accessToken.getExpirationTime().compareTo(new Date()) <= 0;
  }

  /** Fetch the access token for the user credentials. */
  @JsonIgnore
  public AccessToken getUserAccessToken() {
    return GoogleCredentialUtils.getAccessToken(userCredentials);
  }

  /** Fetch the pet SA credentials for this user + current workspace. */
  public void fetchPetSaCredentials() {
    // pet SAs are workspace-specific. if the current workspace is not defined, there is no pet SA
    // to fetch
    GlobalContext globalContext = GlobalContext.get();
    if (globalContext.getCurrentWorkspace().isEmpty()) {
      logger.debug("No current workspace defined. Skipping fetch of pet SA credentials.");
      return;
    }

    // if the key file for this user + workspace already exists, then no need to re-fetch
    Path jsonKeyPath = GlobalContext.get().getPetSaKeyFile();
    logger.debug("Looking for pet SA key file at: {}", jsonKeyPath);
    if (jsonKeyPath.toFile().exists()) {
      logger.debug("Pet SA key file for this user and workspace already exists.");
    } else {
      // ask SAM for the project-specific pet SA key
      HttpUtils.HttpResponse petSaKeySamResponse =
          new SamService(globalContext.getServer(), this)
              .getPetSaKeyForProject(globalContext.requireCurrentWorkspace().googleProjectId);
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
                GlobalContext.get().getPetSaKeyFile().toFile(), petSaKeySamResponse.responseBody);
        logger.debug("Stored pet SA key file for this user and workspace.");
      } catch (IOException ioEx) {
        throw new SystemException(
            "Error writing pet SA key to the global context directory.", ioEx);
      }
    }

    try {
      // create a credentials object from the key
      petSACredentials =
          GoogleCredentialUtils.getServiceAccountCredential(jsonKeyPath.toFile(), SCOPES);
    } catch (IOException ioEx) {
      throw new SystemException(
          "Error reading pet SA credentials from the global context directory.", ioEx);
    }
  }

  /** Delete all pet SA credentials for this user. */
  public void deletePetSaCredentials() {
    File jsonKeysDir = GlobalContext.getPetSaKeyDir(this).toFile();

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

  /** Getter for the Terra user id. */
  @JsonIgnore
  public String getId() {
    return terraUserId;
  }

  /** Getter for the Terra user email. */
  @JsonIgnore
  public String getEmail() {
    return terraUserEmail;
  }

  /** Getter for the Terra user proxy group email. */
  @JsonIgnore
  public String getProxyGroupEmail() {
    return terraProxyGroupEmail;
  }
}
