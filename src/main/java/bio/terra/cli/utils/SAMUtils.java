package bio.terra.cli.utils;

import bio.terra.cli.auth.TerraUser;
import bio.terra.cli.context.GlobalContext;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.api.VersionApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SamVersion;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling SAM endpoints. */
public class SAMUtils {
  private static final Logger logger = LoggerFactory.getLogger(SAMUtils.class);

  private SAMUtils() {}

  /**
   * Build the SAM API client object for the given Terra user and global context.
   *
   * @param terraUser the Terra user whose credentials are supplied to the API client object
   * @param globalContext the global context holds a pointer to the SAM instance
   * @return the API client object for this user
   */
  public static ApiClient getClientForTerraUser(TerraUser terraUser, GlobalContext globalContext) {
    // fetch the user access token
    // this method call will attempt to refresh the token if it's already expired
    AccessToken userAccessToken = terraUser.getUserAccessToken();

    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(globalContext.getSamUri());
    apiClient.setUserAgent("OpenAPI-Generator/1.0.0 java"); // only logs an error in sam
    apiClient.setAccessToken(userAccessToken.getTokenValue());

    return apiClient;
  }

  /**
   * Call the SAM "/version" endpoint to get the version of the server that is currently running.
   *
   * @return the SAM version object
   */
  public static SamVersion getVersion(ApiClient apiClient) {
    VersionApi versionApi = new VersionApi(apiClient);
    try {
      return versionApi.samVersion();
    } catch (ApiException apiEx) {
      logger.error("Error getting SAM version", apiEx);
      return null;
    }
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * @return the SAM user status info object
   */
  public static UserStatusInfo getUserInfo(ApiClient apiClient) {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      return samUsersApi.getUserStatusInfo();
    } catch (ApiException apiEx) {
      logger.error("Error getting user info from SAM.", apiEx);
      return null;
    }
  }

  /**
   * Populate the terra user id and name properties of the given Terra user object.
   *
   * @param terraUser the Terra user whose credentials are supplied to the API client object
   * @param globalContext the global context holds a pointer to the SAM instance
   */
  public static void populateTerraUserInfo(TerraUser terraUser, GlobalContext globalContext) {
    ApiClient samClient = getClientForTerraUser(terraUser, globalContext);
    try {
      UserStatusInfo samUserInfo = getUserInfo(samClient);
      terraUser
          .terraUserId(samUserInfo.getUserSubjectId())
          .terraUserName(samUserInfo.getUserEmail());
    } finally {
      // try to close the connection pool after we're finished with this request -- why is this
      // needed?
      try {
        samClient.getHttpClient().connectionPool().evictAll();
      } catch (Exception anyEx) {
      }
    }
  }

  /**
   * Call the SAM "/api/google/v1/user/petServiceAccount/key" endpoint to get the pet SA key for the
   * current user (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * @return the HTTP response to the SAM request
   */
  public static HTTPUtils.HTTPResponse getPetSAKey(
      TerraUser terraUser, GlobalContext globalContext) {
    // The code below should be changed to use the SAM client library. For example:
    //   GoogleApi samGoogleApi = new GoogleApi(apiClient);
    //   samGoogleApi.getArbitraryPetServiceAccountKey();
    // But I couldn't get this to work. The ApiClient throws an exception, I think in parsing the
    // response. So for now, this is making a direct (i.e. without the client library) HTTP request
    // to get the key file contents.
    try {
      String apiEndpoint = globalContext.getSamUri() + "/api/google/v1/user/petServiceAccount/key";
      String userAccessToken = terraUser.getUserAccessToken().getTokenValue();
      return HTTPUtils.sendHttpRequest(apiEndpoint, "GET", userAccessToken, null);
    } catch (IOException ioEx) {
      logger.error("Error getting pet SA key from SAM.", ioEx);
      return null;
    }
  }
}
