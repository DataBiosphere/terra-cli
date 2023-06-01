package bio.terra.cli.cloud.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Auth0AuthorizationCodeFlow extends AuthorizationCodeFlow {

  public Auth0AuthorizationCodeFlow(
      HttpTransport transport, GoogleClientSecrets googleClientSecrets, String authUrl) {
    super(
        BearerToken.authorizationHeaderAccessMethod(),
        transport,
        JacksonFactory.getDefaultInstance(),
        new GenericUrl(googleClientSecrets.getDetails().getTokenUri()),
        new ClientParametersAuthentication(
            googleClientSecrets.getDetails().getClientId(),
            googleClientSecrets.getDetails().getClientSecret()),
        googleClientSecrets.getDetails().getClientId(),
        authUrl);
  }
}
