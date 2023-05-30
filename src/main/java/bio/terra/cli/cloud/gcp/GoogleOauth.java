package bio.terra.cli.cloud.gcp;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.service.utils.TerraCredentials;
import bio.terra.cli.utils.UserIO;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for manipulating Google credentials. */
public final class GoogleOauth {
  // key names for the credentials persisted in the file data store.
  // the CLI only stores a single set of credentials at a time, so this key is hard-coded here
  // instead of setting to generated ids per user
  public static final String CREDENTIAL_STORE_KEY = "TERRA_USER";
  public static final String ID_TOKEN_STORE_KEY = "TERRA_ID_TOKEN";
  private static final Logger logger = LoggerFactory.getLogger(GoogleOauth.class);
  // google OAuth client secret file
  // (https://developers.google.com/adwords/api/docs/guides/authentication#create_a_client_id_and_client_secret)
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  private GoogleOauth() {}

  /** Load the client secrets file to pass to oauth API's. */
  private static GoogleClientSecrets readRenderedClientSecrets() {
    String clientCredentialsFileName = Context.getServer().getClientCredentialsFile();

    try {
      // Path clientCredentialsFilePath = Path.of(System.getProperty("user.dir"), "rendered",
      // clientCredentialsFileName);
      return GoogleClientSecrets.load(
          GsonFactory.getDefaultInstance(),
          new InputStreamReader(
              new FileInputStream(clientCredentialsFileName), StandardCharsets.UTF_8));
      // new FileInputStream(clientCredentialsFilePath.toAbsolutePath().toFile()),
      // StandardCharsets.UTF_8));

    } catch (IOException ioException) {
      throw new SystemException(
          String.format(
              "Failure reading client secrets from file: '%s'.", clientCredentialsFileName),
          ioException);
    }
  }

  /** Get the client secrets to pass to oauth API's. */
  public static GoogleClientSecrets getClientSecrets() {
    return readRenderedClientSecrets();
  }

  /**
   * Do the Google OAuth2 flow for the specified userId. If there is no existing, unexpired
   * credential for this userId, this method will require browser window to ask for consent to
   * access the specified scopes. This browser window is either launched automatically or the URL
   * printed to stdout, depending on the boolean flag.
   *
   * @param scopes list of scopes to request from the user
   * @param dataStoreDir directory in which to persist the local credential store
   * @param launchBrowserAutomatically true to launch a browser automatically and listen on a local
   *     server for the token response, false to print the url to stdout and ask the user to
   *     copy/paste the token response to stdin
   * @param loginLandingPage URL of the page to load in the browser upon completion of login
   * @return credentials object for the user
   */
  public static TerraCredentials doLoginAndConsent(
      List<String> scopes,
      File dataStoreDir,
      boolean launchBrowserAutomatically,
      String loginLandingPage)
      throws IOException, GeneralSecurityException {

    // setup the Google OAuth2 flow
    TerraAuthenticationHelper helper =
        TerraAuthenticationHelper.create(scopes, readRenderedClientSecrets(), dataStoreDir);
    GoogleAuthorizationCodeFlow flow = helper.getGoogleAuthorizationCodeFlow();

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
          new AuthorizationCodeInstalledApp(
                  flow,
                  new StdinReceiver(
                      readRenderedClientSecrets().getInstalled().getRedirectUris().get(0)),
                  new NoLaunchBrowser())
              .authorize(CREDENTIAL_STORE_KEY);
    }

    if (credential.getRefreshToken() == null || credential.getRefreshToken().isEmpty()) {
      logger.info(
          "Refresh token is not set. This is expected when testing, not during normal operation.");
    } else {
      credential.refreshToken();
    }

    // OAuth2 Credentials representing a user's identity and consent
    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(readRenderedClientSecrets().getDetails().getClientId())
            .setClientSecret(readRenderedClientSecrets().getDetails().getClientSecret())
            .setRefreshToken(credential.getRefreshToken())
            .setAccessToken(
                new AccessToken(
                    credential.getAccessToken(),
                    new Date(credential.getExpirationTimeMilliseconds())))
            .build();

    return new TerraCredentials(credentials, helper.getStoredIdToken());
  }

  /**
   * Delete the credential associated with the specified userId.
   *
   * @param scopes list of scopes requested of the user
   * @param dataStoreDir directory where the local credential store is persisted
   */
  public static void deleteExistingCredential(List<String> scopes, File dataStoreDir)
      throws IOException, GeneralSecurityException {

    // get a pointer to the credential datastore
    TerraAuthenticationHelper helper =
        TerraAuthenticationHelper.create(scopes, readRenderedClientSecrets(), dataStoreDir);
    helper.deleteStoredCredential();
    helper.deleteStoredIdToken();
  }

  /**
   * Get the existing stored credential for the given user if it exists.
   *
   * @param scopes list of scopes requested of the user
   * @param dataStoreDir directory where the local credential store is persisted
   * @return credentials object for the user, or null if a valid stored credential does not exist
   */
  @Nullable
  public static TerraCredentials getExistingUserCredential(List<String> scopes, File dataStoreDir)
      throws IOException, GeneralSecurityException {
    // get a pointer to the credential datastore
    TerraAuthenticationHelper helper =
        TerraAuthenticationHelper.create(scopes, readRenderedClientSecrets(), dataStoreDir);

    // fetch the stored credential for the specified userId
    UserCredentials credentials;
    StoredCredential storedCredential = helper.getStoredCredential();
    if (storedCredential == null) {
      return null; // there is no credential, return here
    } else {

      // now turn the stored credential into a regular OAuth2 Credentials representing a user's
      // identity and consent
      credentials =
          UserCredentials.newBuilder()
              .setClientId(readRenderedClientSecrets().getDetails().getClientId())
              .setClientSecret(readRenderedClientSecrets().getDetails().getClientSecret())
              .setRefreshToken(storedCredential.getRefreshToken())
              .setAccessToken(
                  new AccessToken(
                      storedCredential.getAccessToken(),
                      new Date(storedCredential.getExpirationTimeMilliseconds())))
              .build();
    }

    IdToken idToken = helper.getStoredIdToken();

    if (idToken == null) {
      // We have existing credentials, but do not have an ID token.  Return null in order to force a
      // credential refresh which will capture and store an ID token with the new credentials.
      logger.info("Credentials present, but no ID token present.");
      return null;
    }

    return new TerraCredentials(credentials, idToken);
  }

  /**
   * Get a credentials object for a service account using its JSON-formatted key file.
   *
   * @param jsonKey file handle for the JSON-formatted service account key file
   * @param scopes scopes to request for the credential object
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
   * @param credential Terra credentials object
   * @return access token
   */
  public static AccessToken getAccessToken(TerraCredentials credential) {
    try {
      credential.getGoogleCredentials().refreshIfExpired();
    } catch (IOException ioEx) {
      logger.warn("Error refreshing access token", ioEx);
      // don't throw an exception here because the token may not be expired, in which case it's fine
      // to use it without refreshing first. if the token is expired, then we'll get a permission
      // error when we try to re-use it anyway, and this log statement may help with debugging.
    }
    return credential.getGoogleCredentials().getAccessToken();
  }

  /**
   * @param credentials Terra credentials object
   * @return id token
   */
  public static IdToken getIdToken(TerraCredentials credentials) {
    IdToken idToken = credentials.getIdToken();
    Date now = new Date();
    if (idToken.getExpirationTime().before(now)) {
      // We shouldn't get here based on prior checks, specifically a preceding call to
      // User.requiresReauthentication(), which will trigger a full credential refresh if the ID
      // token is close to expiration.  If we do get here there is no further action we can take
      // since we can't refresh an ID token directly (this must be done using the Google SDK OAuth
      // Flow which obtains both tokens in a single request); the action the user should take is to
      // retry their command, as next run should get new creds and succeed.
      logger.error(
          "ID token expiration ({}) before current time ({}).", idToken.getExpirationTime(), now);
      throw new UserActionableException("ID Token expired, please try your command again.");
    }
    return idToken;
  }

  /**
   * Revoke token (https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke).
   *
   * <p>Google Java OAuth library doesn't support revoking tokens
   * (https://github.com/googleapis/google-oauth-java-client/issues/250), so make the call ourself.
   *
   * @param credential credentials object
   */
  public static void revokeToken(Optional<TerraCredentials> credential) {
    if (credential.isPresent()
        && credential.get().getGoogleCredentials().getAccessToken() != null) {
      String endpoint = "https://oauth2.googleapis.com/revoke";
      Map<String, String> headers =
          ImmutableMap.of("Content-type", "application/x-www-form-urlencoded");
      Map<String, String> params =
          ImmutableMap.of(
              "token", credential.get().getGoogleCredentials().getAccessToken().getTokenValue());

      try {
        HttpUtils.sendHttpRequest(endpoint, "POST", headers, params);
      } catch (IOException ioEx) {
        throw new SystemException("Unable to revoke token", ioEx);
      }
    }
  }

  /**
   * Helper class that asks the user to copy/paste the token response manually to stdin.
   * https://developers.google.com/identity/protocols/oauth2/native-app#step-2:-send-a-request-to-googles-oauth-2.0-server
   */
  private static class StdinReceiver extends AbstractPromptReceiver {
    private final String redirectUri;

    public StdinReceiver(String redirectUri) {
      this.redirectUri = redirectUri;
    }

    @Override
    public String getRedirectUri() {
      return redirectUri;
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
   * Helper class to register with a {@code GoogleAuthorizationCodeFlow} instance to listen for
   * callbacks on creation and refresh of credentials by the flow, and get/store the ID token (which
   * is always returned, but otherwise discarded).
   */
  private static class IdCredentialListener
      implements AuthorizationCodeFlow.CredentialCreatedListener, CredentialRefreshListener {
    final FileDataStoreFactory fileDataStoreFactory;
    final String storeName;
    final String storeKey;

    /**
     * ctor, only called by {@code TerraAuthenticationHelper}
     *
     * @param fileDataStoreFactory factory used to obtain the Data Store used for cred storage
     * @param storeName name of the Data Store used for cred storage
     * @param storeKey key used within the cred Data Store to store/retrieve ID token
     */
    public IdCredentialListener(
        FileDataStoreFactory fileDataStoreFactory, String storeName, String storeKey) {
      this.fileDataStoreFactory = fileDataStoreFactory;
      this.storeName = storeName;
      this.storeKey = storeKey;
    }

    /** Get the stored cred used for storing the ID Token */
    public DataStore<IdToken> getDataStore() throws IOException {
      return fileDataStoreFactory.getDataStore(storeName);
    }

    /**
     * Parse an ID token from a token request's response and upsert into the ID token cred store;
     * called on token create and refresh
     */
    private void storeIdToken(TokenResponse tokenResponse) throws IOException {
      IdToken idToken = IdToken.create(tokenResponse.get("id_token").toString());
      getDataStore().set(storeKey, idToken);
    }

    /** Callback called on token creation */
    @Override
    public void onCredentialCreated(Credential credential, TokenResponse tokenResponse)
        throws IOException {
      storeIdToken(tokenResponse);
    }

    /** Callback called on token refresh */
    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse)
        throws IOException {
      storeIdToken(tokenResponse);
    }

    /** Callback called on token refresh failure */
    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) {
      throw new SystemException("Error obtaining token: " + tokenErrorResponse);
    }
  }

  /**
   * Helper class that creates and binds together several objects required for the OAuth token flow
   * across different authentication modalities: the {@code GoogleAuthorizationCodeFlow}, the {@code
   * IdCredentialListener} instance required to obtain an ID Token from the flow, and the Credential
   * store used for both storing and retrieving credentials.
   */
  private static class TerraAuthenticationHelper {

    private final IdCredentialListener idCredentialListener;
    private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;

    /**
     * Private ctor called by create() method to create and associate idCredentialListener and
     * googleAuthorizationCodeFlow members.
     */
    private TerraAuthenticationHelper(
        List<String> scopes, GoogleClientSecrets clientSecrets, File dataStoreDir)
        throws IOException, GeneralSecurityException {

      // get a pointer to the credential datastore
      FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(dataStoreDir);

      // create an IdCredentialListener pointing at the credential datastore in order to store and
      // retrieve ID tokens
      idCredentialListener =
          new IdCredentialListener(
              fileDataStoreFactory, StoredCredential.DEFAULT_DATA_STORE_ID, ID_TOKEN_STORE_KEY);

      // create the code flow object, pointing at the credential datastore in order to store and
      // retrieve ID tokens, and registering our IdCredentialListener for callbacks on token
      // creation/refresh
      googleAuthorizationCodeFlow =
          new GoogleAuthorizationCodeFlow.Builder(
                  GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets, scopes)
              .setDataStoreFactory(fileDataStoreFactory)
              .setAccessType("offline")
              .setApprovalPrompt("force")
              .setCredentialCreatedListener(idCredentialListener)
              .addRefreshListener(idCredentialListener)
              .build();
    }

    /**
     * Create an instance of {@code TerraAuthenticationHelper}, which is used both for initiating an
     * OAuth browser flow, as well as storing and retrieving credentials in a DataStore.
     *
     * @param scopes OAuth scopes to request when launching an OAuth authentication flow
     * @param clientSecrets Application Client Secrets used to obtain credentials on behalf of CLI
     * @param dataStoreDir Directory where credentials store lives, used for cred storage/retrieval
     * @return TerraAuthenticationHelper
     * @throws IOException IOException
     * @throws GeneralSecurityException GeneralSecurityException
     */
    public static TerraAuthenticationHelper create(
        List<String> scopes, GoogleClientSecrets clientSecrets, File dataStoreDir)
        throws IOException, GeneralSecurityException {
      return new TerraAuthenticationHelper(scopes, clientSecrets, dataStoreDir);
    }

    /** DRY helper for deleting from DataStore objects parameterized for different types. */
    private static <T extends Serializable> void deleteFromDataStore(
        DataStore<T> dataStore, String key) throws IOException {
      if (!dataStore.containsKey(key)) {
        logger.debug("Credential for {} not found.", key);
      }
      dataStore.delete(key);
    }

    public GoogleAuthorizationCodeFlow getGoogleAuthorizationCodeFlow() {
      return googleAuthorizationCodeFlow;
    }

    /** Delete stored ID token from the credential store */
    public void deleteStoredIdToken() throws IOException {
      deleteFromDataStore(idCredentialListener.getDataStore(), ID_TOKEN_STORE_KEY);
    }

    /** Delete stored GoogleCredential (access and refresh tokens) from the credential store */
    public void deleteStoredCredential() throws IOException {
      deleteFromDataStore(
          googleAuthorizationCodeFlow.getCredentialDataStore(), CREDENTIAL_STORE_KEY);
    }

    /** Get ID token from credential store. */
    public IdToken getStoredIdToken() throws IOException {
      return idCredentialListener.getDataStore().get(ID_TOKEN_STORE_KEY);
    }

    /** Get GoogleCrendential (access and refresh tokens) from the credential store */
    public StoredCredential getStoredCredential() throws IOException {
      return googleAuthorizationCodeFlow.getCredentialDataStore().get(CREDENTIAL_STORE_KEY);
    }
  }
}
