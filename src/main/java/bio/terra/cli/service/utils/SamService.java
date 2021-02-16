package bio.terra.cli.service.utils;

import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatus;
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
   * Call the SAM "/status" endpoint to get the status of the server.
   *
   * @return the SAM status object, null if there was an error checking the status
   */
  public SystemStatus getStatus() {
    StatusApi statusApi = new StatusApi(apiClient);
    SystemStatus status = null;
    try {
      status =
          HttpUtils.callWithRetries(() -> statusApi.getSystemStatus(), (ex) -> isRetryable(ex));
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
   * <p>Update the Terra User object passed in with the user information from SAM.
   */
  public void getUser() throws Exception {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      UserStatusInfo userStatusInfo =
          HttpUtils.callWithRetries(() -> samUsersApi.getUserStatusInfo(), (ex) -> isRetryable(ex));
      terraUser.terraUserId = userStatusInfo.getUserSubjectId();
      terraUser.terraUserEmail = userStatusInfo.getUserEmail();
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * <p>If that returns a Not Found error, then call the SAM "register/user/v2/self" endpoint to
   * register the user.
   *
   * <p>Update the Terra User object with the user information from SAM.
   */
  public void getOrRegisterUser() throws Exception {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      // first try to lookup the user
      UserStatusInfo userStatusInfo =
          HttpUtils.callWithRetries(() -> samUsersApi.getUserStatusInfo(), (ex) -> isRetryable(ex));
      terraUser.terraUserId = userStatusInfo.getUserSubjectId();
      terraUser.terraUserEmail = userStatusInfo.getUserEmail();
    } catch (ApiException apiEx) {
      if (apiEx.getCode() != HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        throw apiEx;
      }
      logger.info("User not found in SAM. Trying to register a new user.");

      // lookup failed with Not Found error, now try to register the user
      UserStatus userStatus =
          HttpUtils.callWithRetries(() -> samUsersApi.createUserV2(), (ex) -> isRetryable(ex));
      terraUser.terraUserId = userStatus.getUserInfo().getUserSubjectId();
      terraUser.terraUserEmail = userStatus.getUserInfo().getUserEmail();
    } finally {
      closeConnectionPool();
    }
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
   * Call the SAM "/api/google/v1/user/petServiceAccount/{project}/key" endpoint to get a
   * project-specific pet SA key for the current user (i.e. the one whose credentials were supplied
   * to the apiClient object).
   *
   * @param workspaceContext the current workspace
   * @return the HTTP response to the SAM request
   */
  public HttpUtils.HttpResponse getPetSaKeyForProject(WorkspaceContext workspaceContext)
      throws Exception {
    // The code below should be changed to use the SAM client library. For example:
    //  ApiClient apiClient = getClientForTerraUser(terraUser, globalContext.server);
    //  GoogleApi samGoogleApi = new GoogleApi(apiClient);
    //  samGoogleApi.getPetServiceAccount(workspaceContext.getGoogleProject());
    // But I couldn't get this to work. The ApiClient throws an exception, I think in parsing the
    // response. So for now, this is making a direct (i.e. without the client library) HTTP request
    // to get the key file contents.
    String apiEndpoint =
        server.samUri
            + "/api/google/v1/user/petServiceAccount/"
            + workspaceContext.getGoogleProject()
            + "/key";
    String userAccessToken = terraUser.fetchUserAccessToken().getTokenValue();
    return HttpUtils.callWithRetries(
        () -> {
          HttpUtils.HttpResponse response =
              HttpUtils.sendHttpRequest(apiEndpoint, "GET", userAccessToken, null);
          if (HttpStatusCodes.isSuccess(response.statusCode)) {
            return response;
          }
          throw new ApiException(
              response.statusCode,
              "Error calling /api/google/v1/user/petServiceAccount/{project}/key endpoint");
        },
        (ex) -> isRetryable(ex));
  }

  /**
   * Utility method that checks if an exception thrown by the SAM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  private static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    int statusCode = ((ApiException) ex).getCode();
    return statusCode == HttpStatusCodes.STATUS_CODE_SERVER_ERROR
        || statusCode == HttpStatusCodes.STATUS_CODE_SERVICE_UNAVAILABLE;
  }
}
