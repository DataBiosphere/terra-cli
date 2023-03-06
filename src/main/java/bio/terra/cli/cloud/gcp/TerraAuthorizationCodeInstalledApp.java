package bio.terra.cli.cloud.gcp;
/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static bio.terra.cli.cloud.gcp.GoogleOauth.ID_TOKEN_STORE_KEY;

import bio.terra.cli.businessobject.Context;
import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.TokenHolder;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.auth.oauth2.IdToken;
import java.io.IOException;

/**
 * OAuth 2.0 authorization code flow for an installed Java application that persists end-user
 * credentials.
 *
 * <p>Implementation is thread-safe.
 *
 * @since 1.11
 * @author Yaniv Inbar
 * @author Philipp Hanslovsky
 */
public class TerraAuthorizationCodeInstalledApp extends AuthorizationCodeInstalledApp {

  public TerraAuthorizationCodeInstalledApp(
      AuthorizationCodeFlow flow, VerificationCodeReceiver receiver) {
    super(flow, receiver);
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
      System.out.println(redirectUri);
      AuthorizationCodeRequestUrl authorizationUrl =
          getFlow().newAuthorizationUrl().setRedirectUri(redirectUri);
      System.out.println(authorizationUrl.build());
      onAuthorization(authorizationUrl);
      // receive authorization code and exchange it for an access token
      String code = getReceiver().waitForCode();
      System.out.println(code);
      AuthAPI auth =
          new AuthAPI(
              "terra-sandbox.us.auth0.com",
              "9iX5StTZSzVGTxwPw94F47pCG77AXpeF",
              "61Dmg1ut-N60GjYfBokmpSSoK4__5BnPo3jzA4dAaDU133u7_DqqxGbfY4cSQMQJ");
      TokenHolder result =
          auth.exchangeCode(code, "http://localhost:3000")
              .setScope("openid profile email")
              .setAudience("https://terra-devel.api.verily.com")
              .execute();

      var tokenResponse =
          new TokenResponse()
              .setAccessToken(result.getAccessToken())
              .setExpiresInSeconds(result.getExpiresIn())
              .setRefreshToken(result.getRefreshToken())
              .setScope(result.getScope())
              .setTokenType(result.getTokenType());
      new FileDataStoreFactory(Context.getContextDir().toFile())
          .getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID)
          .set(ID_TOKEN_STORE_KEY, IdToken.create(result.getIdToken()));
      return getFlow().createAndStoreCredential(tokenResponse, userId);
    } finally {
      getReceiver().stop();
    }
  }
}
