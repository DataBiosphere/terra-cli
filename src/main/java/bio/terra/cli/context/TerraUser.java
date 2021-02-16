package bio.terra.cli.context;

import bio.terra.cli.auth.GoogleCredentialUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import java.io.File;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This POJO class represents a Terra identity context, which includes all related credentials (e.g.
 * user, pet SA).
 */
public class TerraUser {
  private static final Logger logger = LoggerFactory.getLogger(TerraUser.class);

  // The CLI generates a random UUID for each user, to keep track of which credentials belong to
  // which user. (Note: It would be better to use the SAM/Terra subject id, but we don't have that
  // until after logging in because we need Google credentials to get the subject id from SAM.)
  public String cliGeneratedUserKey;

  // This field stores the id that Terra uses to identify a user. The CLI queries SAM for a user's
  // subject id to populate this field.
  public String terraUserId;

  // This field stores the name that Terra associates with this user. The CLI queries SAM for a
  // user's email to populate this field.
  public String terraUserEmail;

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
    logger.info("Access token expiration date: {}", accessToken.getExpirationTime());
    return accessToken.getExpirationTime().compareTo(new Date()) <= 0;
  }

  /** Fetch the access token for the user credentials. */
  public AccessToken fetchUserAccessToken() {
    return GoogleCredentialUtils.getAccessToken(userCredentials);
  }

  /** Return a pointer to the pet key file for the given Google project id. */
  public File getPetKeyFile(String googleProjectId) {
    // TODO: move each user's keys into its own sub-directory (easier to mount on container), and
    // maintain an index by project
    return GlobalContext.resolvePetSaKeyDir().resolve(terraUserId).toFile();
  }
}
