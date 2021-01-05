package bio.terra.cli.auth;

import bio.terra.cli.utils.AuthenticationUtils;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerraUser {
  private static final Logger logger = LoggerFactory.getLogger(TerraUser.class);

  private String terraUserId;
  private String terraUserName;
  private GoogleCredentials userCredentials;
  private ServiceAccountCredentials petSACredentials;

  public TerraUser(String terraUserId) {
    this.terraUserId = terraUserId;
  }

  /** Getter for the Terra user id. */
  public String getTerraUserId() {
    return terraUserId;
  }

  /** Getter for the Terra user name. */
  public String getTerraUserName() {
    return terraUserName;
  }

  /** Chainable setter for the Terra user name. */
  public TerraUser terraUserName(String terraUserName) {
    this.terraUserName = terraUserName;
    return this;
  }

  /** Getter for the user credentials for this Terra user. */
  public GoogleCredentials getUserCredentials() {
    return userCredentials;
  }

  /** Chainable setter for the user credentials for this Terra user. */
  public TerraUser userCredentials(GoogleCredentials userCredentials) {
    this.userCredentials = userCredentials;
    return this;
  }

  /** Getter for the pet SA credentials for this Terra user. */
  public ServiceAccountCredentials getPetSACredentials() {
    return petSACredentials;
  }

  /** Chainable setter for the pet SA credentials for this Terra user. */
  public TerraUser petSACredentials(ServiceAccountCredentials petSACredentials) {
    this.petSACredentials = petSACredentials;
    return this;
  }

  /** Check if the user credentials are expired. */
  public boolean requiresReauthentication() {
    // if the user credentials are not defined, then we need to re-authenticate
    if (userCredentials == null) {
      return true;
    }

    // fetch the access token
    // this method call will attempt to refresh the token if it's already expired
    AccessToken accessToken = AuthenticationUtils.getAccessToken(userCredentials);

    // check if the token is expired
    logger.debug("Access token expiration date: {}", accessToken.getExpirationTime());
    boolean tokenIsExpired = accessToken.getExpirationTime().compareTo(new Date()) <= 0;

    return tokenIsExpired;
  }
}
