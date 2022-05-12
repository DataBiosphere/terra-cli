package bio.terra.cli.service.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdToken;

/**
 * Wrapper class to hold a user's credential to use when calling Terra API. Consists of a
 * GoogleCredential object containing access and refresh tokens and an IdToken object containing a
 * JWT ID token.
 */
public class TerraCredentials {
  private final GoogleCredentials googleCredentials;
  private final IdToken idToken;

  public TerraCredentials(GoogleCredentials googleCredentials, IdToken idToken) {
    if (googleCredentials == null || idToken == null) {
      throw new NullPointerException("GoogleCredential and IdToken must both be non-null.");
    }
    this.googleCredentials = googleCredentials;
    this.idToken = idToken;
  }

  public GoogleCredentials getGoogleCredentials() {
    return googleCredentials;
  }

  public IdToken getIdToken() {
    return idToken;
  }
}
