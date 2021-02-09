package bio.terra.cli.service.utils;

import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import com.google.auth.oauth2.AccessToken;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.api.VersionApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SamVersion;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling SAM endpoints. */
public class SamService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  // the Terra environment where the SAM service lives
  private final ServerSpecification server;

  // the Terra user whose credentials will be used to call authenticated requests
  private final TerraUser terraUser;

  // the client object used for talking to SAM
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to the SAM service. The user must be authenticated. Methods in
   * this class will use its credentials to call authenticated endpoints.
   *
   * @param server the Terra environment where the SAM service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  public SamService(ServerSpecification server, TerraUser terraUser) {
    this.server = server;
    this.terraUser = terraUser;
    this.apiClient = new ApiClient();
    buildClientForTerraUser();
  }

  /**
   * Constructor for class that talks to the SAM service. No user is specified, so only
   * unauthenticated endpoints can be called.
   *
   * @param server the Terra environment where the SAM service lives
   */
  public SamService(ServerSpecification server) {
    this(server, null);
  }

  /**
   * Build the SAM API client object for the given Terra user and global context. If terraUser is
   * null, this method builds the client object without an access token set.
   */
  private void buildClientForTerraUser() {
    this.apiClient.setBasePath(server.samUri);
    this.apiClient.setUserAgent("OpenAPI-Generator/1.0.0 java"); // only logs an error in sam

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.fetchUserAccessToken();
      this.apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
  }

  /**
   * Call the SAM "/version" endpoint to get the version of the server that is currently running.
   *
   * @return the SAM version object
   */
  public SamVersion getVersion() {
    VersionApi versionApi = new VersionApi(apiClient);
    SamVersion samVersion = null;
    try {
      samVersion = versionApi.samVersion();
    } catch (Exception ex) {
      logger.error("Error getting SAM version", ex);
    } finally {
      closeConnectionPool();
    }
    return samVersion;
  }

  /**
   * Call the SAM "/status" endpoint to get the status of the server.
   *
   * @return the SAM status object
   */
  public SystemStatus getStatus() {
    StatusApi statusApi = new StatusApi(apiClient);
    SystemStatus status = null;
    try {
      status = statusApi.getSystemStatus();
    } catch (Exception ex) {
      logger.error("Error getting SAM status", ex);
    } finally {
      closeConnectionPool();
    }
    return status;
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * @return the SAM user status info object
   */
  public UserStatusInfo getUserInfo() {
    UsersApi samUsersApi = new UsersApi(apiClient);
    UserStatusInfo userStatusInfo = null;
    try {
      userStatusInfo = samUsersApi.getUserStatusInfo();
    } catch (Exception ex) {
      logger.error("Error getting user info from SAM.", ex);
    } finally {
      closeConnectionPool();
    }
    return userStatusInfo;
  }

  /**
   * Populate the terra user id and name properties of the given Terra user object. Maps the SAM
   * subject id to the user id, and the SAM email to the user name.
   */
  public void populateTerraUserInfo() {
    UserStatusInfo samUserInfo = getUserInfo();
    terraUser.terraUserId = samUserInfo.getUserSubjectId();
    terraUser.terraUserEmail = samUserInfo.getUserEmail();
  }

  /** Try to close the connection pool after we're finished with this SAM request. */
  private void closeConnectionPool() {
    // try to close the connection pool after we're finished with this request
    // TODO: why is this needed? possibly a bad interaction with picoCLI?
    try {
      apiClient.getHttpClient().connectionPool().evictAll();
    } catch (Exception anyEx) {
      logger.debug(
          "Error forcing connection pool to shutdown after making a SAM client library call.",
          anyEx);
    }
  }

  /**
   * Call the SAM "/api/google/v1/user/petServiceAccount/key" endpoint to get an arbitrary pet SA
   * key for the current user (i.e. the one whose credentials were supplied to the apiClient
   * object).
   *
   * @return the HTTP response to the SAM request
   */
  public HttpUtils.HttpResponse getArbitraryPetSaKey() {
    // The code below should be changed to use the SAM client library. For example:
    //   GoogleApi samGoogleApi = new GoogleApi(apiClient);
    //   samGoogleApi.getArbitraryPetServiceAccountKey();
    // But I couldn't get this to work. The ApiClient throws an exception, I think in parsing the
    // response. So for now, this is making a direct (i.e. without the client library) HTTP request
    // to get the key file contents.
    try {
      String apiEndpoint = server.samUri + "/api/google/v1/user/petServiceAccount/key";
      String userAccessToken = terraUser.fetchUserAccessToken().getTokenValue();
      return HttpUtils.sendHttpRequest(apiEndpoint, "GET", userAccessToken, null);
    } catch (Exception ex) {
      logger.error("Error getting arbitrary pet SA key from SAM.", ex);
      return null;
    }
  }

  /**
   * Call the SAM "/api/google/v1/user/petServiceAccount/{project}/key" endpoint to get a
   * project-specific pet SA key for the current user (i.e. the one whose credentials were supplied
   * to the apiClient object).
   *
   * @return the HTTP response to the SAM request
   */
  public HttpUtils.HttpResponse getPetSaKeyForProject(WorkspaceContext workspaceContext) {
    // The code below should be changed to use the SAM client library. For example:
    //  ApiClient apiClient = getClientForTerraUser(terraUser, globalContext.server);
    //  GoogleApi samGoogleApi = new GoogleApi(apiClient);
    //  samGoogleApi.getPetServiceAccount(workspaceContext.getGoogleProject());
    // But I couldn't get this to work. The ApiClient throws an exception, I think in parsing the
    // response. So for now, this is making a direct (i.e. without the client library) HTTP request
    // to get the key file contents.
    try {
      String apiEndpoint =
          server.samUri
              + "/api/google/v1/user/petServiceAccount/"
              + workspaceContext.getGoogleProject()
              + "/key";
      String userAccessToken = terraUser.fetchUserAccessToken().getTokenValue();
      return HttpUtils.sendHttpRequest(apiEndpoint, "GET", userAccessToken, null);
    } catch (Exception ex) {
      logger.error("Error getting project-specific pet SA key from SAM.", ex);
      return null;
    }
  }
}
