package bio.terra.cli.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating Google credentials. */
public final class GoogleCredentialUtils {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCredentialUtils.class);

  private GoogleCredentialUtils() {}

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  /**
   * Do the Google OAuth2 flow for the specified userId. If there is no existing, unexpired
   * credential for this userId, this method will open a browser window to ask for consent to access
   * the specified scopes.
   */
  public static UserCredentials doLoginAndConsent(
      String userId, List<String> scopes, InputStream clientSecretFile, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, Charset.forName("UTF-8")));

    // setup the Google OAuth2 flow
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);

    // exchange an authorization code for a refresh token
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(userId);

    // OAuth2 Credentials representing a user's identity and consent
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(clientSecrets.getDetails().getClientId())
            .setClientSecret(clientSecrets.getDetails().getClientSecret())
            .setRefreshToken(credential.getRefreshToken())
            .build();
    credentials.refresh();

    return credentials;
  }

  /** Delete the credential associated with the specified userId. */
  public static void deleteExistingCredential(
      String userId, List<String> scopes, InputStream clientSecretFile, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, Charset.forName("UTF-8")));

    // get a pointer to the credential datastore
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);
    DataStore<StoredCredential> dataStore = flow.getCredentialDataStore();

    // check that the specified credential exists
    if (!dataStore.containsKey(userId)) {
      logger.info("Credential for {} not found.", userId);
      return;
    }

    // remove the specified credential
    dataStore.delete(userId);
  }

  /** Get the existing credential for the given user. */
  public static UserCredentials getExistingUserCredential(
      String userId, List<String> scopes, InputStream clientSecretFile, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // load client_secret.json file
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY, new InputStreamReader(clientSecretFile, Charset.forName("UTF-8")));

    // get a pointer to the credential datastore
    GoogleAuthorizationCodeFlow flow = getOAuth2Flow(scopes, clientSecrets, dataStoreDir);
    DataStore<StoredCredential> dataStore = flow.getCredentialDataStore();

    // fetch the stored credential for the specified userId
    StoredCredential storedCredential = dataStore.get(userId);
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
            .build();

    return credentials;
  }

  /**
   * Build the GoogleAuthorizationCodeFlow object for the given scopes, client secret and data store
   * directory.
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

  /** Get a credentials object for a service account using its JSON-formatted key file. */
  public static ServiceAccountCredentials getServiceAccountCredential(
      File jsonKey, List<String> scopes) throws IOException {
    return (ServiceAccountCredentials)
        ServiceAccountCredentials.fromStream(new FileInputStream(jsonKey)).createScoped(scopes);
  }

  /**
   * Refresh the credential if expired and then return its access token.
   *
   * @param credential
   * @return access token
   */
  public static AccessToken getAccessToken(GoogleCredentials credential) {
    try {
      credential.refreshIfExpired();
      return credential.getAccessToken();
    } catch (IOException ioEx) {
      throw new RuntimeException("Error refreshing access token", ioEx);
    }
  }
}
