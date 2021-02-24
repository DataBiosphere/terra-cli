package bio.terra.cli.auth;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.service.utils.SamService;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manipulates the auth-related properties of the global context object. */
@SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification =
        "Known false positive for certain try-with-resources blocks, which are used in several methods in this class. https://github.com/spotbugs/spotbugs/issues/1338 (Other similar issues linked from there.)")
public class AuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);

  public static final List<String> SCOPES =
      Collections.unmodifiableList(
          Arrays.asList(
              "openid", "email", "profile", "https://www.googleapis.com/auth/cloud-platform"));

  private static final String CLIENT_SECRET_FILENAME = "jadecli_client_secret.json";

  private GlobalContext globalContext;
  private WorkspaceContext workspaceContext;

  public AuthenticationManager(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
  }

  /**
   * Fetch all credentials for a new user, and set them as the current Terra user. Prompt for login
   * and consent if they do not already exist or are expired.
   */
  public void loginTerraUser() {
    // this will become the current Terra user if we are successful in getting all the various
    // information and credentials below
    TerraUser terraUser;
    Optional<TerraUser> currentTerraUser = globalContext.getCurrentTerraUser();
    if (!currentTerraUser.isPresent()) {
      // generate a random string to use as the CLI user identifer
      // it would be better to use the SAM subject id here instead, but we can't talk to SAM until
      // we have Google (user) credentials, and we need to give the Google login flow an identifer
      // for storing the credential in a persistent file. so, this is the id that we hand to Google
      // and can use later to look up the persisted Google (user) credentials
      String cliGeneratedUserKey = UUID.randomUUID().toString();
      terraUser = new TerraUser(cliGeneratedUserKey);
    } else {
      terraUser = currentTerraUser.get();
    }

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthenticationManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // otherwise, log the user in and get their consent
      userCredentials =
          GoogleCredentialUtils.doLoginAndConsent(
              terraUser.cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile(),
              globalContext.launchBrowserAutomatically);
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    terraUser.userCredentials = userCredentials;

    // fetch the user information from SAM, if it's not already populated
    if (!currentTerraUser.isPresent()) {
      new SamService(globalContext.server, terraUser).getOrRegisterUser();
    }
    fetchPetSaCredentials(terraUser);

    // update the global context with the current user, if it's changed
    if (!currentTerraUser.isPresent()) {
      globalContext.addOrUpdateTerraUser(terraUser, true);
    }
  }

  /** Delete all credentials associated with the current Terra user. */
  public void logoutTerraUser() {
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();
    if (!currentTerraUserOpt.isPresent()) {
      logger.debug("There is no current Terra user defined in the global context.");
      return;
    }
    TerraUser currentTerraUser = currentTerraUserOpt.get();

    try (InputStream inputStream =
        AuthenticationManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // delete the user credentials
      GoogleCredentialUtils.deleteExistingCredential(
          currentTerraUser.cliGeneratedUserKey,
          SCOPES,
          inputStream,
          globalContext.resolveGlobalContextDir().toFile());

      // delete the pet SA credentials
      deletePetSaCredentials(currentTerraUser);
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error deleting credentials.", ex);
    }
  }

  /**
   * Populates user and SA credentials, and user information for the current Terra user. If there is
   * no current user defined, or their credentials are expired, then this method does not populate
   * anything.
   */
  public void populateCurrentTerraUser() {
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();
    if (!currentTerraUserOpt.isPresent()) {
      logger.info("There is no current Terra user defined in the global context.");
      return;
    }
    TerraUser currentTerraUser = currentTerraUserOpt.get();

    // fetch existing user credentials
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthenticationManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // fetch any non-expired existing credentials for this user
      userCredentials =
          GoogleCredentialUtils.getExistingUserCredential(
              currentTerraUser.cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile());

      // if there are no valid credentials, then return here because there's nothing to populate
      if (userCredentials == null) {
        return;
      }
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    currentTerraUser.userCredentials = userCredentials;

    // fetch the user information from SAM
    new SamService(globalContext.server, currentTerraUser).getUser();
    if (currentTerraUser.terraUserId != null) {
      // fetch the pet SA credentials if they don't already exist
      fetchPetSaCredentials(currentTerraUser);
    }

    // update the global context with the populated information
    globalContext.addOrUpdateTerraUser(currentTerraUser);
  }

  /** Fetch the pet SA credentials for the given user + current workspace. */
  public void fetchPetSaCredentials(TerraUser terraUser) {
    // if the current workspace is not defined, then we don't know which pet SA to fetch
    if (workspaceContext.isEmpty()) {
      logger.info("There is no current workspace defined.");
      return;
    }

    // if the key file for this user + workspace already exists, then no need to re-fetch
    Path jsonKeyPath = GlobalContext.getPetSaKeyFile(terraUser, workspaceContext.getWorkspaceId());
    logger.info("Looking for pet SA key file at: {}", jsonKeyPath);
    if (jsonKeyPath.toFile().exists()) {
      logger.info("Pet SA key file for this user and workspace already exists.");
    } else {
      // ask SAM for the project-specific pet SA key
      HttpUtils.HttpResponse petSaKeySamResponse =
          new SamService(globalContext.server, terraUser).getPetSaKeyForProject(workspaceContext);
      if (!HttpStatusCodes.isSuccess(petSaKeySamResponse.statusCode)) {
        logger.info("SAM response to pet SA key request: {})", petSaKeySamResponse.responseBody);
        throw new RuntimeException(
            "Error fetching pet SA key from SAM (status code = "
                + petSaKeySamResponse.statusCode
                + ").");
      }
      try {
        // persist the key file in the global context directory
        jsonKeyPath =
            FileUtils.writeStringToFile(
                GlobalContext.getPetSaKeyDirForUser(terraUser),
                GlobalContext.getPetSaKeyFilename(workspaceContext.getWorkspaceId()),
                petSaKeySamResponse.responseBody);
        logger.info("Stored pet SA key file for this user and workspace.");
      } catch (IOException ioEx) {
        throw new RuntimeException(
            "Error writing pet SA key to the global context directory.", ioEx);
      }
    }

    try {
      // create a credentials object from the key
      ServiceAccountCredentials petSaCredentials =
          GoogleCredentialUtils.getServiceAccountCredential(jsonKeyPath.toFile(), SCOPES);
      terraUser.petSACredentials = petSaCredentials;
    } catch (IOException ioEx) {
      throw new RuntimeException(
          "Error reading pet SA credentials from the global context directory.", ioEx);
    }
  }

  /** Delete all pet SA credentials for the given user. */
  public void deletePetSaCredentials(TerraUser terraUser) {
    File jsonKeysDir = GlobalContext.getPetSaKeyDirForUser(terraUser).toFile();

    // delete all key files
    File[] keyFiles = jsonKeysDir.listFiles();
    if (keyFiles != null) {
      for (File keyFile : keyFiles) {
        if (!keyFile.delete() && keyFile.exists()) {
          throw new RuntimeException(
              "Failed to delete pet SA key file: " + keyFile.getAbsolutePath());
        }
      }
    }

    // delete the key file directory
    if (!jsonKeysDir.delete() && jsonKeysDir.exists()) {
      throw new RuntimeException(
          "Failed to delete pet SA key file sub-directory: " + jsonKeysDir.getAbsolutePath());
    }
  }
}
