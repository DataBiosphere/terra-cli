package bio.terra.cli.utils;

import bio.terra.cli.model.ServerSpecification;
import bio.terra.cli.model.TerraUser;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.SystemStatus;
import bio.terra.workspace.model.SystemVersion;
import com.google.auth.oauth2.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerUtils {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerUtils.class);

  private WorkspaceManagerUtils() {}

  /**
   * Build the Workspace Manager API client object for the given Terra user and global context. If
   * terraUser is null, this method returns the client object without an access token set.
   *
   * @param terraUser the Terra user whose credentials are supplied to the API client object
   * @param server the server specification that holds a pointer to the Workspace Manger instance
   * @return the API client object for this user
   */
  public static ApiClient getClientForTerraUser(TerraUser terraUser, ServerSpecification server) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(server.workspaceManagerUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.fetchUserAccessToken();
      apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
    return apiClient;
  }

  /**
   * Call the Workspace Manager "/version" endpoint to get the version of the server that is
   * currently running.
   *
   * @return the Workspace Manager version object
   */
  public static SystemVersion getVersion(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    SystemVersion systemVersion = null;
    try {
      systemVersion = unauthenticatedApi.serviceVersion();
    } catch (ApiException apiEx) {
      logger.error("Error getting Workspace Manager version", apiEx);
    }
    return systemVersion;
  }

  /**
   * Call the Workspace Manager "/status" endpoint to get the status of the server.
   *
   * @return the Workspace Manager status object
   */
  public static SystemStatus getStatus(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    SystemStatus status = null;
    try {
      status = unauthenticatedApi.serviceStatus();
    } catch (ApiException apiEx) {
      logger.error("Error getting Workspace Manager status", apiEx);
    }
    return status;
  }
}
