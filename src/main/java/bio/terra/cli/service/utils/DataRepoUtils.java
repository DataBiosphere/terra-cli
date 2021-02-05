package bio.terra.cli.service.utils;

import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.datarepo.api.UnauthenticatedApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.model.RepositoryConfigurationModel;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.google.auth.oauth2.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Data Repo endpoints. */
public class DataRepoUtils {
  private static final Logger logger = LoggerFactory.getLogger(DataRepoUtils.class);

  private DataRepoUtils() {}

  /**
   * Build the Data Repo API client object for the given Terra user and global context. If terraUser
   * is null, this method returns the client object without an access token set.
   *
   * @param terraUser the Terra user whose credentials are supplied to the API client object
   * @param server the server specification that holds a pointer to the Data Repo instance
   * @return the API client object for this user
   */
  public static ApiClient getClientForTerraUser(TerraUser terraUser, ServerSpecification server) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(server.dataRepoUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.fetchUserAccessToken();
      apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
    return apiClient;
  }

  /**
   * Call the Data Repo "/configuration" endpoint to get the version of the server that is currently
   * running.
   *
   * @return the Data Repo configuration object
   */
  public static RepositoryConfigurationModel getVersion(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    RepositoryConfigurationModel repositoryConfig = null;
    try {
      repositoryConfig = unauthenticatedApi.retrieveRepositoryConfig();
    } catch (Exception ex) {
      logger.error("Error getting Data Repo configuration", ex);
    }
    return repositoryConfig;
  }

  /**
   * Call the Data Repo "/status" endpoint to get the status of the server.
   *
   * @return the Data Repo status object
   */
  public static RepositoryStatusModel getStatus(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    RepositoryStatusModel status = null;
    try {
      status = unauthenticatedApi.serviceStatus();
    } catch (Exception ex) {
      logger.error("Error getting Data Repo status", ex);
    }
    return status;
  }
}
