package bio.terra.cli.cloud.auth;

import bio.terra.cli.businessobject.Context;
import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.TokenRequest;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0AuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {

  private final GoogleClientSecrets secrets;

  public Auth0AuthorizationCodeInstalledApp(
      AuthorizationCodeFlow flow, VerificationCodeReceiver receiver, GoogleClientSecrets secrets) {
    super(flow, receiver);
    this.secrets = secrets;
  }

  public Auth0AuthorizationCodeInstalledApp(
      AuthorizationCodeFlow flow,
      VerificationCodeReceiver receiver,
      AuthorizationCodeInstalledApp.Browser browser,
      GoogleClientSecrets secrets) {
    super(flow, receiver, browser);
    this.secrets = secrets;
  }

  /**
   * Authorizes the installed application to access user's protected data.
   *
   * @param userId user ID or {@code null} if not using a persisted credential store
   * @return credential
   * @throws IOException
   */
  public Credential authorize(String userId) throws IOException {
    try {
      Credential credential = getFlow().loadCredential(userId);
      if (credential != null
          && (credential.getRefreshToken() != null
              || credential.getExpiresInSeconds() == null
              || credential.getExpiresInSeconds() > 60)) {
        return credential;
      }
      // open in browser
      String redirectUri = getReceiver().getRedirectUri();
      AuthorizationCodeRequestUrl authorizationUrl =
          getFlow().newAuthorizationUrl().setRedirectUri(redirectUri);
      onAuthorization(authorizationUrl);
      String code = getReceiver().waitForCode();

      AuthAPI authAPI =
          new AuthAPI(
              Context.getServer().getAuth0Domain(),
              secrets.getDetails().getClientId(),
              secrets.getDetails().getClientSecret());
      TokenRequest tokenRequest = authAPI.exchangeCode(code, "http://localhost:3000");
      TokenHolder result = tokenRequest.execute().getBody();

      var tokenResponse =
          new TokenResponse()
              .setAccessToken(result.getAccessToken())
              .setExpiresInSeconds(result.getExpiresIn())
              .setRefreshToken(result.getRefreshToken())
              .setScope(result.getScope())
              .setTokenType(result.getTokenType())
              .set("id_token", result.getIdToken());

      return getFlow().createAndStoreCredential(tokenResponse, userId);
    } finally {
      getReceiver().stop();
    }
  }
}
