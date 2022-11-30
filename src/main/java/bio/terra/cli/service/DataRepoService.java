package bio.terra.cli.service;

import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.exception.SystemException;
import bio.terra.datarepo.api.UnauthenticatedApi;
import bio.terra.datarepo.client.ApiClient;
import bio.terra.datarepo.client.ApiException;
import bio.terra.datarepo.model.RepositoryConfigurationModel;
import bio.terra.datarepo.model.RepositoryStatusModel;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Data Repo endpoints. */
public class DataRepoService {
  private static final Logger logger = LoggerFactory.getLogger(DataRepoService.class);
  // the client object used for talking to Data Repo
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to TDR. If the user is null, only unauthenticated endpoints
   * can be called.
   */
  private DataRepoService(@Nullable User user, Server server) {
    this.apiClient = new ApiClient();

    this.apiClient.setBasePath(server.getDataRepoUri());
    if (user != null) {
      this.apiClient.setAccessToken(user.getTerraToken().getTokenValue());
    }
  }

  /**
   * Factory method for class that talks to TDR. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static DataRepoService unauthenticated(Server server) {
    return new DataRepoService(null, server);
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
