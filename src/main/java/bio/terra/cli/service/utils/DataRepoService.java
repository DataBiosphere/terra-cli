package bio.terra.cli.service.utils;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.Server;
import bio.terra.cli.context.TerraUser;
import bio.terra.datarepo.api.UnauthenticatedApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.client.ApiException;
import bio.terra.datarepo.model.RepositoryConfigurationModel;
import bio.terra.datarepo.model.RepositoryStatusModel;
import com.google.auth.oauth2.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Data Repo endpoints. */
public class DataRepoService {
  private static final Logger logger = LoggerFactory.getLogger(DataRepoService.class);

  // the client object used for talking to Data Repo
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to the Data Repo service. The user must be authenticated.
   * Methods in this class will use its credentials to call authenticated endpoints.
   *
   * @param server the Terra environment where the Data Repo service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  public DataRepoService(Server server, TerraUser terraUser) {
    this.apiClient = new ApiClient();
    buildClientForTerraUser(server, terraUser);
  }

  /**
   * Constructor for class that talks to the Data Repo service. No user is specified, so only
   * unauthenticated endpoints can be called.
   *
   * @param server the Terra environment where the Data Repo service lives
   */
  public DataRepoService(Server server) {
    this(server, null);
  }

  /**
   * Build the Data Repo API client object for the given Terra user and global context. If terraUser
   * is null, this method builds the client object without an access token set.
   *
   * @param server the Terra environment where the Data Repo service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  private void buildClientForTerraUser(Server server, TerraUser terraUser) {
    this.apiClient.setBasePath(server.dataRepoUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.getUserAccessToken();
      this.apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
  }

  /**
   * Call the Data Repo "/configuration" endpoint to get the version of the server that is currently
   * running.
   *
   * @return the Data Repo configuration object
   */
  public RepositoryConfigurationModel getVersion() {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    RepositoryConfigurationModel repositoryConfig = null;
    try {
      repositoryConfig = unauthenticatedApi.retrieveRepositoryConfig();
    } catch (ApiException ex) {
      throw new SystemException("Error getting Data Repo version", ex);
    }
    return repositoryConfig;
  }

  /**
   * Call the Data Repo "/status" endpoint to get the status of the server.
   *
   * @return the Data Repo status object
   */
  public RepositoryStatusModel getStatus() {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    try {
      return unauthenticatedApi.serviceStatus();
    } catch (ApiException ex) {
      throw new SystemException("Error getting Data Repo status", ex);
    }
  }
}
