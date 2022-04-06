package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.externalcreds.api.SshKeyPairApi;
import bio.terra.externalcreds.client.ApiClient;
import bio.terra.externalcreds.model.SshKeyPair;
import bio.terra.externalcreds.model.SshKeyPairType;
import com.google.auth.oauth2.AccessToken;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class ExternalCredentialsManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);

  // the Terra environment where the WSM service lives
  private final Server server;

  // the client object used for talking to WSM
  private final ApiClient apiClient;

  private ExternalCredentialsManagerService(@Nullable AccessToken accessToken, Server server) {
    this.server = server;
    this.apiClient =
        new ApiClient(new RestTemplate(List.of(new MappingJackson2HttpMessageConverter())));

    this.apiClient.setBasePath(server.getExternalCredsUri());
    if (accessToken != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
  }

  public static ExternalCredentialsManagerService returnFromContext() {
    return new ExternalCredentialsManagerService(
        Context.requireUser().getUserAccessToken(), Context.getServer());
  }

  /**
   * Gets Ssh key pair from ECM. If one does not exist yet, generate one.
   *
   * @param keyPairType git server tied to the key (e.g. GitHub, GitLab, Azure).
   */
  public SshKeyPair getSshKeyPair(SshKeyPairType keyPairType) {
    SshKeyPairApi sshKeyPairApi = new SshKeyPairApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> sshKeyPairApi.getSshKeyPair(keyPairType),
          ExternalCredentialsManagerService::isRetryable);
    } catch (HttpStatusCodeException | InterruptedException e) {
      if (e instanceof HttpStatusCodeException) {
        if (((HttpStatusCodeException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
          return callWithRetries(
              () -> sshKeyPairApi.generateSshKeyPair(Context.requireUser().getEmail(), keyPairType),
              "failed to regenerate an ssh key");
        }
        throw (HttpStatusCodeException) e;
      }
      throw new SystemException("Failed to get ssh key", e);
    }
  }

  /** Regenerate an SshKey for the user. */
  public SshKeyPair regenerateSshKeyPair(SshKeyPairType keyPairType) {
    SshKeyPairApi sshKeyPairApi = new SshKeyPairApi(apiClient);
    return callWithRetries(
        () -> sshKeyPairApi.generateSshKeyPair(Context.requireUser().getEmail(), keyPairType),
        "Failed to regenerate ssh key");
  }

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, HttpStatusCodeException> makeRequest,
      String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(makeRequest, ExternalCredentialsManagerService::isRetryable),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting ECM endpoints. If an exception is thrown by the WSM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, HttpStatusCodeException> makeRequest,
      String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (HttpStatusCodeException | InterruptedException ex) {
      // if this is an ECN client exception, check for a message in the response body
      if (ex instanceof HttpStatusCodeException) {
        String exceptionErrorMessage = logErrorMessage((HttpStatusCodeException) ex);

        errorMsg += ": " + exceptionErrorMessage;
      }

      // wrap the ECM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  private static boolean isRetryable(Exception ex) {
    if (!(ex instanceof HttpStatusCodeException)) {
      return false;
    }
    logErrorMessage((HttpStatusCodeException) ex);
    var statusCode = ((HttpStatusCodeException) ex).getStatusCode();
    return statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.BAD_GATEWAY
        || statusCode == HttpStatus.SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.GATEWAY_TIMEOUT;
  }

  /** Pull a human-readable error message from an ApiException. */
  private static String logErrorMessage(HttpStatusCodeException ex) {
    logger.error(
        "ECM exception status code: {}, response body: {}, message: {}",
        ex.getStatusCode(),
        ex.getResponseBodyAsString(),
        ex.getMessage());

    // try to deserialize the response body into an ErrorReport
    var responseBody = ex.getResponseBodyAsString();

    // if we found a SAM error message, then return it
    // otherwise return a string with the http code
    return !TextUtils.isEmpty(responseBody)
        ? responseBody
        : ex.getStatusCode() + " " + ex.getMessage();
  }
}
