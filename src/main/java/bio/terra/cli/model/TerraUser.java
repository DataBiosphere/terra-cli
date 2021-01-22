package bio.terra.cli.model;

import bio.terra.cli.utils.AuthenticationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents a Terra identity context, which includes all related credentials (e.g.
 * user, pet SA).
 */
public class TerraUser {
  private static final Logger logger = LoggerFactory.getLogger(TerraUser.class);

  public String cliGeneratedUserKey;
  public String terraUserId;
  public String terraUserName;
  @JsonIgnore public UserCredentials userCredentials;
  @JsonIgnore public ServiceAccountCredentials petSACredentials;

  public TerraUser() {}

  public TerraUser(String cliGeneratedUserKey) {
    this.cliGeneratedUserKey = cliGeneratedUserKey;
  }

  /** Check if the user credentials are expired. */
  public boolean requiresReauthentication() {
    // if the user credentials are not defined, then we need to re-authenticate
    if (userCredentials == null) {
      return true;
    }

    // fetch the user access token
    // this method call will attempt to refresh the token if it's already expired
    AccessToken accessToken = fetchUserAccessToken();

    // check if the token is expired
    logger.debug("Access token expiration date: {}", accessToken.getExpirationTime());
    boolean tokenIsExpired = accessToken.getExpirationTime().compareTo(new Date()) <= 0;

    return tokenIsExpired;
  }

  /** Fetch the access token for the user credentials. */
  public AccessToken fetchUserAccessToken() {
    return AuthenticationUtils.getAccessToken(userCredentials);
  }
}
