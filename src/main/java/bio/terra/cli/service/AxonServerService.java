package bio.terra.cli.service;

import bio.terra.axonserver.api.GcpResourceApi;
import bio.terra.axonserver.api.PublicApi;
import bio.terra.axonserver.client.ApiClient;
import bio.terra.axonserver.client.ApiException;
import bio.terra.axonserver.model.ClusterMetadata;
import bio.terra.axonserver.model.ClusterStatus;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.HttpUtils;
import com.google.auth.oauth2.AccessToken;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Axon Server endpoints. */
public class AxonServerService {
  private static final Logger logger = LoggerFactory.getLogger(AxonServerService.class);
  // the client object used for talking to Axon Server
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to Axon Server. If the accessToken is null, only
   * unauthenticated endpoints can be called.
   */
  private AxonServerService(@Nullable AccessToken accessToken, Server server) {
    this.apiClient = new ApiClient();

    this.apiClient.setBasePath(server.getAxonServerUri());
    if (accessToken != null) {
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
  }

  public static AxonServerService fromContext() {
    return new AxonServerService(Context.requireUser().getTerraToken(), Context.getServer());
  }

  /**
   * Factory method for class that talks to Axon Server. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static AxonServerService unauthenticated(Server server) {
    return new AxonServerService(null, server);
  }

  /** Call the Axon Server "/status" endpoint to get the status of the server. */
  public void getStatus() {
    PublicApi publicApi = new PublicApi(apiClient);
    try {
      publicApi.serviceStatus();
    } catch (ApiException ex) {
      throw new SystemException("Error getting Axon Server status", ex);
    }
  }

  /**
   * Call the Axon Server GET
   * /api/workspaces/v1/{workspaceId}/resources/{resourceId}/gcp/dataproccluster/metadata:
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the cluster resource id
   */
  public ClusterMetadata getClusterMetadata(UUID workspaceId, UUID resourceId) {
    GcpResourceApi gcpResourceApi = new GcpResourceApi(apiClient);
    return callWithRetries(
        () -> gcpResourceApi.getDataprocClusterMetadata(workspaceId, resourceId),
        "Failed to retrieve cluster metadata");
  }

  /**
   * Call the Axon Server GET
   * /api/workspaces/v1/{workspaceId}/resources/{resourceId}/gcp/dataproccluster/componentUrl:
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the cluster resource id
   * @param componentKey the component key, e.g. "JupyterLab"
   */
  public bio.terra.axonserver.model.Url getClusterComponentUrl(
      UUID workspaceId, UUID resourceId, String componentKey) {
    GcpResourceApi gcpResourceApi = new GcpResourceApi(apiClient);
    return callWithRetries(
        () -> gcpResourceApi.getDataprocClusterComponentUrl(workspaceId, resourceId, componentKey),
        "Failed to retrieve cluster component url");
  }

  /**
   * Call the Axon Server GET
   * /api/workspaces/v1/{workspaceId}/resources/{resourceId}/gcp/dataproccluster/status:
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the cluster resource id
   */
  public ClusterStatus getClusterStatus(UUID workspaceId, UUID resourceId) {
    GcpResourceApi gcpResourceApi = new GcpResourceApi(apiClient);
    return callWithRetries(
        () -> gcpResourceApi.getDataprocClusterStatus(workspaceId, resourceId),
        "Failed to retrieve cluster status");
  }

  /**
   * Call the Axon Server GET
   * /api/workspaces/v1/{workspaceId}/resources/{resourceId}/gcp/dataproccluster/start:
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the cluster resource id
   */
  public void putStartCluster(UUID workspaceId, UUID resourceId) {
    GcpResourceApi gcpResourceApi = new GcpResourceApi(apiClient);
    callWithRetries(
        () -> gcpResourceApi.putDataprocClusterStart(workspaceId, resourceId, /* wait=*/ false),
        "Failed to start cluster");
  }

  /**
   * Call the Axon Server GET
   * /api/workspaces/v1/{workspaceId}/resources/{resourceId}/gcp/dataproccluster/stop:
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the cluster resource id
   */
  public void putStopCluster(UUID workspaceId, UUID resourceId) {
    GcpResourceApi gcpResourceApi = new GcpResourceApi(apiClient);
    callWithRetries(
        () -> gcpResourceApi.putDataprocClusterStart(workspaceId, resourceId, /* wait=*/ false),
        "Failed to stop cluster");
  }

  /**
   * Utility method that checks if an exception thrown by the Axon Server client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  private static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    logErrorMessage((ApiException) ex);
    int statusCode = ((ApiException) ex).getCode();
    return statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
  }

  /** Pull a human-readable error message from an ApiException. */
  private static String logErrorMessage(ApiException ex) {
    logger.error(
        "Axon Server exception status code: {}, response body: {}, message: {}",
        ex.getCode(),
        ex.getResponseBody(),
        ex.getMessage());

    // try to deserialize the response body into an ErrorReport
    String responseBody = ex.getResponseBody();

    // if we found a Axon Server error message, then return it
    // otherwise return a string with the http code
    return !TextUtils.isEmpty(responseBody) ? responseBody : ex.getCode() + " " + ex.getMessage();
  }

  /**
   * Execute a function that includes hitting Axon Server endpoints. Retry if the function throws an
   * {@link #isRetryable} exception. If an exception is thrown by the Axon Server client or the
   * retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the WSM client or the retries
   */
  private void callWithRetries(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> {
          HttpUtils.callWithRetries(makeRequest, AxonServerService::isRetryable);
          return null;
        },
        errorMsg);
  }

  /**
   * Execute a function that includes hitting Axon Server endpoints. Retry if the function throws an
   * {@link #isRetryable} exception. If an exception is thrown by the User client or the retries,
   * make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the User client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, AxonServerService::isRetryable), errorMsg);
  }

  /**
   * Execute a function that includes hitting Axon Server endpoints. If an exception is thrown by
   * the Axon Server client or the retries, make sure the HTTP status code and error message are
   * logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the Axon Server client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is an Axon Server client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        errorMsg += ": " + logErrorMessage((ApiException) ex);
      }

      // wrap the Axon Server exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }
}
