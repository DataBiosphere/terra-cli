package bio.terra.cli.cloud.gcp;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import java.util.Collection;

public class TerraAuthorizationCodeFlow extends GoogleAuthorizationCodeFlow {

  private final String approvalPrompt;

  /**
   * Access type ({@code "online"} to request online access or {@code "offline"} to request offline
   * access) or {@code null} for the default behavior.
   */
  private final String accessType;

  public TerraAuthorizationCodeFlow(HttpTransport transport,
      JsonFactory jsonFactory, String clientId, String clientSecret,
      Collection<String> scopes) {
    this(new Builder(transport, jsonFactory, clientId, clientSecret, scopes));
  }

  protected TerraAuthorizationCodeFlow(Builder builder) {
    super(builder);
    accessType = builder.getAccessType();
    approvalPrompt = builder.getApprovalPrompt();
  }

  @Override
  public GoogleAuthorizationCodeTokenRequest newTokenRequest(String authorizationCode) {
    // don't need to specify clientId & clientSecret because specifying clientAuthentication
    // don't want to specify redirectUri to give control of it to user of this class
    return new GoogleAuthorizationCodeTokenRequest(
        getTransport(),
        getJsonFactory(),
        getTokenServerEncodedUrl(),
        "",
        "",
        authorizationCode,
        "")
        .setClientAuthentication(getClientAuthentication())
        .setRequestInitializer(getRequestInitializer())
        .setScopes(getScopes());
  }

  @Override
  public GoogleAuthorizationCodeRequestUrl newAuthorizationUrl() {
    // don't want to specify redirectUri to give control of it to user of this class
    return new GoogleAuthorizationCodeRequestUrl(
        getAuthorizationServerEncodedUrl(), getClientId(), "", getScopes())
        .setAccessType(accessType)
        .setApprovalPrompt(approvalPrompt);
  }
}
