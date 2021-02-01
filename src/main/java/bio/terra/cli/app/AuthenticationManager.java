package bio.terra.cli.app;

import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.TerraUser;
import bio.terra.cli.utils.AuthenticationUtils;
import bio.terra.cli.utils.FileUtils;
import bio.terra.cli.utils.HttpUtils;
import bio.terra.cli.utils.SamUtils;
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

  private final GlobalContext globalContext;

  public AuthenticationManager(GlobalContext globalContext) {
    this.globalContext = globalContext;
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
          AuthenticationUtils.doLoginAndConsent(
              terraUser.cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    terraUser.userCredentials = userCredentials;

    // fetch the user information from SAM
    SamUtils.populateTerraUserInfo(terraUser, globalContext);

    // fetch the pet SA credentials from SAM
    HttpUtils.HttpResponse petSAKeySAMResponse = SamUtils.getPetSaKey(terraUser, globalContext);
    if (petSAKeySAMResponse.statusCode != 200) {
      logger.debug("SAM response to pet SA key request: {})", petSAKeySAMResponse.responseBody);
      throw new RuntimeException(
          "Error fetching pet SA key from SAM (status code = "
              + petSAKeySAMResponse.statusCode
              + ").");
    }
    try {
      // persist it in the global context directory
      Path jsonKeyFile =
          FileUtils.writeStringToFile(
              globalContext.resolvePetSAKeyDir(),
              terraUser.terraUserId,
              petSAKeySAMResponse.responseBody);

      // create a credentials object from the key
      ServiceAccountCredentials petSACredentials =
          AuthenticationUtils.getServiceAccountCredential(jsonKeyFile.toFile(), SCOPES);

      terraUser.petSACredentials = petSACredentials;
    } catch (IOException ioEx) {
      logger.error("Error writing pet SA key to the global context directory.", ioEx);
    }

    // update the global context with the current user
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    globalContext.addOrUpdateTerraUser(terraUser);
    globalContext.setCurrentTerraUser(terraUser);
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
      AuthenticationUtils.deleteExistingCredential(
          currentTerraUser.cliGeneratedUserKey,
          SCOPES,
          inputStream,
          globalContext.resolveGlobalContextDir().toFile());

      // delete the pet SA credentials
      File jsonKeyFile =
          globalContext.resolvePetSAKeyDir().resolve(currentTerraUser.terraUserId).toFile();
      if (!jsonKeyFile.delete()) {
        throw new RuntimeException("Failed to delete pet SA key file.");
      }
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error deleting credentials.", ex);
    }
  }

  /**
   * Populates user and SA credentials for the current Terra user. Returns early if no current Terra
   * user has been set.
   */
  public void populateCurrentTerraUser() {
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();
    if (!currentTerraUserOpt.isPresent()) {
      logger.debug("There is no current Terra user defined in the global context.");
      return;
    }
    TerraUser currentTerraUser = currentTerraUserOpt.get();

    // fetch the user credentials, prompt for login and consent if they do not already exist or are
    // expired.
    UserCredentials userCredentials;
    try (InputStream inputStream =
        AuthenticationManager.class.getClassLoader().getResourceAsStream(CLIENT_SECRET_FILENAME)) {

      // if there are already credentials for this user, and they are not expired, then return them
      // if there are no valid credentials for this user, then this method will return null
      userCredentials =
          AuthenticationUtils.getExistingUserCredential(
              currentTerraUser.cliGeneratedUserKey,
              SCOPES,
              inputStream,
              globalContext.resolveGlobalContextDir().toFile());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException("Error fetching user credentials.", ex);
    }
    currentTerraUser.userCredentials = userCredentials;

    // if there are existing user credentials, then try to fetch the existing pet SA credentials
    // also
    if (userCredentials != null) {
      File jsonKeyFile =
          globalContext.resolvePetSAKeyDir().resolve(currentTerraUser.terraUserId).toFile();
      if (!jsonKeyFile.exists() || !jsonKeyFile.isFile()) {
        throw new RuntimeException("Pet SA key file not found.");
      }
      try {
        ServiceAccountCredentials petSACredentials =
            AuthenticationUtils.getServiceAccountCredential(jsonKeyFile, SCOPES);
        currentTerraUser.petSACredentials = petSACredentials;
      } catch (IOException ioEx) {
        throw new RuntimeException("Error reading pet SA key file.", ioEx);
      }
    }
  }

  /**
   * Utility wrapper around populateCurrentTerraUser that throws an exception if the current Terra
   * user is not defined.
   */
  public TerraUser requireCurrentTerraUser() {
    Optional<TerraUser> terraUserOpt = globalContext.getCurrentTerraUser();
    if (!terraUserOpt.isPresent()) {
      throw new RuntimeException("The current Terra user is not defined. Login required.");
    }
    populateCurrentTerraUser();
    return terraUserOpt.get();
  }
}
