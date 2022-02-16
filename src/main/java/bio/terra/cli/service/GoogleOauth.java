package bio.terra.cli.service;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.UserIO;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating Google credentials. */
public final class GoogleOauth {
  private static final Logger logger = LoggerFactory.getLogger(GoogleOauth.class);

  // key name for the single credential persisted in the file data store.
  // the CLI only stores a single credential at a time, so this key is hard-coded here instead of
  // setting it to a generated id per user
  public static final String CREDENTIAL_STORE_KEY = "TERRA_USER";

  private GoogleOauth() {}

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  /**
   * Do the Google OAuth2 flow for the specified userId. If there is no existing, unexpired
   * credential for this userId, this method will require browser window to ask for consent to
   * access the specified scopes. This browser window is either launched automatically or the URL
   * printed to stdout, depending on the boolean flag.
   *
   * @param scopes list of scopes to request from the user
   * @param clientSecretFile stream to the client secret file
   * @param dataStoreDir directory in which to persist the local credential store
   * @param launchBrowserAutomatically true to launch a browser automatically and listen on a local
   *     server for the token response, false to print the url to stdout and ask the user to
   *     copy/paste the token response to stdin
   * @param loginLandingPage URL of the page to load in the browser upon completion of login
   * @return credentials object for the user
   */
  public static UserCredentials doLoginAndConsent(
      List<String> scopes,
      InputStream clientSecretFile,
      File dataStoreDir,
      boolean launchBrowserAutomatically,
      String loginLandingPage)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, StandardCharsets.UTF_8));

    // setup the Google OAuth2 flow
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);

    // exchange an authorization code for a refresh token
    Credential credential;
    if (launchBrowserAutomatically) {
      // launch a browser window on this machine and listen on a local port for the token response
      LocalServerReceiver receiver =
          new LocalServerReceiver.Builder()
              .setLandingPages(loginLandingPage, loginLandingPage)
              .build();
      credential =
          new AuthorizationCodeInstalledApp(flow, receiver).authorize(CREDENTIAL_STORE_KEY);
    } else {
      // print the url to stdout and ask the user to copy/paste the token response to stdin
      credential =
          new AuthorizationCodeInstalledApp(flow, new StdinReceiver(), new NoLaunchBrowser())
              .authorize(CREDENTIAL_STORE_KEY);
    }

    // OAuth2 Credentials representing a user's identity and consent
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(clientSecrets.getDetails().getClientId())
            .setClientSecret(clientSecrets.getDetails().getClientSecret())
            .setRefreshToken(credential.getRefreshToken())
            .setAccessToken(
                new AccessToken(
                    credential.getAccessToken(),
                    new Date(credential.getExpirationTimeMilliseconds())))
            .build();

    // only try to refresh if the refresh token is set
    if (credentials.getRefreshToken() == null || credentials.getRefreshToken().isEmpty()) {
      logger.info(
          "Refresh token is not set. This is expected when testing, not during normal operation.");
    } else {
      credentials.refresh();
    }

    return credentials;
  }

  /**
   * Helper class that asks the user to copy/paste the token response manually to stdin.
   * https://developers.google.com/identity/protocols/oauth2/native-app#step-2:-send-a-request-to-googles-oauth-2.0-server
   */
  private static class StdinReceiver extends AbstractPromptReceiver {
    @Override
    public String getRedirectUri() {
      return "urn:ietf:wg:oauth:2.0:oob";
    }
  }

  /**
   * Helper class that prints the URL to follow the OAuth flow to stdout, and does not try to open a
   * browser locally (i.e. on this machine).
   */
  private static class NoLaunchBrowser implements AuthorizationCodeInstalledApp.Browser {
    @Override
    public void browse(String url) {
      PrintStream out = UserIO.getOut();
      out.println("Please open the following address in a browser on any machine:");
      out.println("  " + url);
    }
  }

  /**
   * Delete the credential associated with the specified userId.
   *
   * @param scopes list of scopes requested of the user
   * @param clientSecretFile stream to the client secret file
   * @param dataStoreDir directory where the local credential store is persisted
   */
  public static void deleteExistingCredential(
      List<String> scopes, InputStream clientSecretFile, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, StandardCharsets.UTF_8));

    // get a pointer to the credential datastore
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);
    DataStore<StoredCredential> dataStore = flow.getCredentialDataStore();

    // check that the specified credential exists
    if (!dataStore.containsKey(CREDENTIAL_STORE_KEY)) {
      logger.debug("Credential for {} not found.", CREDENTIAL_STORE_KEY);
      return;
    }

    // remove the specified credential
    dataStore.delete(CREDENTIAL_STORE_KEY);
  }

  /**
   * Get the existing credential for the given user.
   *
   * @param scopes list of scopes requested of the user
   * @param clientSecretFile stream to the client secret file
   * @param dataStoreDir directory where the local credential store is persisted
   * @return credentials object for the user
   */
  public static UserCredentials getExistingUserCredential(
      List<String> scopes, InputStream clientSecretFile, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, StandardCharsets.UTF_8));

    // get a pointer to the credential datastore
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);
    DataStore<StoredCredential> dataStore = flow.getCredentialDataStore();

    // fetch the stored credential for the specified userId
    StoredCredential storedCredential = dataStore.get(CREDENTIAL_STORE_KEY);
    if (storedCredential == null) {
      return null; // there is no credential, return here
    }

    // now turn the stored credential into a regular OAuth2 Credentials representing a user's
    // identity and consent
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(clientSecrets.getDetails().getClientId())
            .setClientSecret(clientSecrets.getDetails().getClientSecret())
            .setRefreshToken(storedCredential.getRefreshToken())
            .setAccessToken(
                new AccessToken(
                    storedCredential.getAccessToken(),
                    new Date(storedCredential.getExpirationTimeMilliseconds())))
            .build();

    return credentials;
  }

  /**
   * Build the GoogleAuthorizationCodeFlow object for the given scopes, client secret and data store
   * directory.
   *
   * @param scopes list of scopes to request from the user
   * @param clientSecrets wrapper object for the client secret file
   * @param dataStoreDir directory in which to persist the local credential store
   * @return oauth flow object
   */
  private static GoogleAuthorizationCodeFlow getOAuth2Flow(
      List<String> scopes, GoogleClientSecrets clientSecrets, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // get a pointer to the credential datastore
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, scopes)
            .setDataStoreFactory(new FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .setApprovalPrompt("force")
            .build();
    return flow;
  }

  /**
   * Get a credentials object for a service account using its JSON-formatted key file.
   *
   * @jsonKey file handle for the JSON-formatted service account key file
   * @scopes scopes to request for the credential object
   * @return credentials object for the service account
   */
  public static ServiceAccountCredentials getServiceAccountCredential(
      File jsonKey, List<String> scopes) throws IOException {
    return (ServiceAccountCredentials)
        ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey)).createScoped(scopes);
  }

  /**
   * Refresh the credential if expired and then return its access token.
   *
   * @param credential credentials object
   * @return access token
   */
  public static AccessToken getAccessToken(GoogleCredentials credential) {
    try {
      credential.refreshIfExpired();
    } catch (IOException ioEx) {
      logger.warn("Error refreshing access token", ioEx);
      // don't throw an exception here because the token may not be expired, in which case it's fine
      // to use it without refreshing first. if the token is expired, then we'll get a permission
      // error when we try to re-use it anyway, and this log statement may help with debugging.
    }
    return credential.getAccessToken();
  }

  /**
   * Revoke token (https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke).
   *
   * <p>Google Java OAuth library doesn't support revoking tokens
   * (https://github.com/googleapis/google-oauth-java-client/issues/250), so make the call ourself.
   *
   * @param credential credentials object
   */
  public static void revokeToken(Optional<GoogleCredentials> credential) {
    if (credential.isPresent() && credential.get().getAccessToken() != null) {
      String endpoint = "https://oauth2.googleapis.com/revoke";
      Map<String, String> headers =
          ImmutableMap.of("Content-type", "application/x-www-form-urlencoded");
      Map<String, String> params =
          ImmutableMap.of("token", credential.get().getAccessToken().getTokenValue());

      try {
        HttpUtils.sendHttpRequest(endpoint, "POST", headers, params);
      } catch (IOException ioEx) {
        throw new SystemException("Unable to revoke token", ioEx);
      }
    }
  }
}
