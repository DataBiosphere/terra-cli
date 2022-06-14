package bio.terra.cli.app.utils;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.GoogleOauth;
import bio.terra.cli.service.utils.TerraCredentials;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for working with Google application default credentials. */
public class AppDefaultCredentialUtils {
  private static final Logger logger = LoggerFactory.getLogger(AppDefaultCredentialUtils.class);

  /** Return the absolute path to file backing the current application default credentials. */
  public static Optional<Path> getADCBackingFile() {
    // Check $HOME/.config/gcloud/application_default_credentials.json
    // this path, if it exists, typically points to user credentials generated by `gcloud auth
    // application-default login`
    Path gcloudConfigPath = getDefaultGcloudADCFile();
    if (gcloudConfigPath.toFile().exists()) {
      logger.debug("adcBackingFile from gcloud config dir: {}", gcloudConfigPath.toAbsolutePath());
      return Optional.of(gcloudConfigPath.toAbsolutePath());
    }

    // there is no file backing ADC
    logger.debug("no adcBackingFile");
    return Optional.empty();
  }

  /**
   * Return the absolute path to the default Gcloud location for storing end-user (i.e. not SA)
   * application default credentials.
   */
  public static Path getDefaultGcloudADCFile() {
    return Path.of(
            System.getProperty("user.home"), ".config/gcloud/application_default_credentials.json")
        .toAbsolutePath();
  }

  /**
   * Throw an exception if the application default credentials are not defined or do not match the
   * current user or their pet SA
   */
  public static void throwIfADCDontMatchContext() {
    if (!doAdcMatchContext()) {
      throw new UserActionableException(
          "Application default credentials do not match the user or pet SA emails.");
    }
  }

  /**
   * Return true if the application default credentials match the pet SA for the current
   * user+workspace, or if they are end-user credentials, which we can't validate directly. Throw an
   * exception if they are not defined.
   */
  private static boolean doAdcMatchContext() {
    GoogleCredentials appDefaultCreds = getApplicationDefaultCredentials();

    if (appDefaultCreds instanceof UserCredentials) {
      logger.info("ADC are end-user credentials. Skipping account/email validation.");
      return true;
    }

    String email = null;
    if (appDefaultCreds instanceof ServiceAccountCredentials) {
      email = ((ServiceAccountCredentials) appDefaultCreds).getClientEmail();
    } else if (appDefaultCreds instanceof ComputeEngineCredentials) {
      email = ((ComputeEngineCredentials) appDefaultCreds).getAccount();
    } else if (appDefaultCreds instanceof ImpersonatedCredentials) {
      email = ((ImpersonatedCredentials) appDefaultCreds).getAccount();
    }
    return email != null && email.equalsIgnoreCase(Context.requireUser().getPetSaEmail());
  }

  /** Get the application default credentials. Throw an exception if they are not defined. */
  private static GoogleCredentials getApplicationDefaultCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException ioEx) {
      throw new UserActionableException(
          "Application default credentials are not defined. Run `gcloud auth application-default login`",
          ioEx);
    }
  }

  /**
   * Get an ID token from an Application Default Credential and Client Secrets. Note that the passed
   * ADC must be properly scoped; passing improperly scoped credentials will result in a {@code
   * SystemException}. Any other failure to obtain the token will result in an {@code IoException}.
   */
  private static IdToken getIdTokenFromADC(GoogleCredentials applicationDefaultCredentials)
      throws IOException {
    if (!(applicationDefaultCredentials instanceof IdTokenProvider)) {
      throw new SystemException(
          "Passed credential is not an IdTokenProvider, please ensure only scoped ADC are passed.");
    }

    // To obtain an ID token from ADC, the ADC must be properly scoped and the OAuth2 Client ID must
    // be passed as the target audience

    IdTokenCredentials idTokenCredentials =
        IdTokenCredentials.newBuilder()
            .setIdTokenProvider((IdTokenProvider) applicationDefaultCredentials)
            .setTargetAudience(GoogleOauth.getClientSecrets().getDetails().getClientId())
            .setOptions(List.of(IdTokenProvider.Option.FORMAT_FULL))
            .build();

    // Must call refresh() to obtain the token.
    idTokenCredentials.refresh();
    return idTokenCredentials.getIdToken();
  }

  public static TerraCredentials getExistingADC(List<String> scopes) throws IOException {
    GoogleCredentials applicationDefaultCredentials =
        AppDefaultCredentialUtils.getApplicationDefaultCredentials().createScoped(scopes);

    return new TerraCredentials(
        applicationDefaultCredentials,
        AppDefaultCredentialUtils.getIdTokenFromADC(applicationDefaultCredentials));
  }
}
