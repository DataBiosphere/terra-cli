package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.user.api.ProfileApi;
import bio.terra.user.api.PublicApi;
import bio.terra.user.client.ApiClient;
import bio.terra.user.client.ApiException;
import bio.terra.user.model.AnyObject;
import com.google.auth.oauth2.AccessToken;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpStatus;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling User Manager endpoints. */
public class UserManagerService {
  private static final Logger logger = LoggerFactory.getLogger(UserManagerService.class);
  // the client object used for talking to User Manager
  private final ApiClient apiClient;

  public static final String SPEND_PROFILE_PATH = "spend_profile";

  /**
   * Constructor for class that talks to User Manager. If the accessToken is null, only
   * unauthenticated endpoints can be called.
   */
  private UserManagerService(@Nullable AccessToken accessToken, Server server) {
    this.apiClient = new ApiClient();

    this.apiClient.setBasePath(server.getUserManagerUri());
    if (accessToken != null) {
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
  }

  public static UserManagerService fromContext() {
    return new UserManagerService(Context.requireUser().getTerraToken(), Context.getServer());
  }

  /**
   * Factory method for class that talks to User Manager. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static UserManagerService unauthenticated(Server server) {
    return new UserManagerService(null, server);
  }

  /** Call the User Manager "/status" endpoint to get the status of the server. */
  public void getStatus() {
    PublicApi publicApi = new PublicApi(apiClient);
    try {
      publicApi.serviceStatus();
    } catch (ApiException ex) {
      throw new SystemException("Error getting User Manager status", ex);
    }
  }

  /**
   * Call the User Manager GET "/api/profile" to retrieve the user profile obejct.
   *
   * @param path the path in the profile object
   * @param email user profile to target (admin only)
   */
  private AnyObject getUserProfile(@Nullable String path, @Nullable String email) {
    ProfileApi profileApi = new ProfileApi(apiClient);
    return callWithRetries(
        () -> profileApi.getUserProfile(path, email), "Failed to get user profile");
  }

  /**
   * Call the User Manager PUT "/api/profile" to set part of the user profile object.
   *
   * @param path the path in the profile object (cannot be empty)
   * @param anyObject the value to set
   * @param email user profile to target (admin only)
   */
  private AnyObject setUserProfile(String path, AnyObject anyObject, @Nullable String email) {
    ProfileApi profileApi = new ProfileApi(apiClient);
    return callWithRetries(
        () -> profileApi.setUserProfile(anyObject, path, email), "Failed to set user profile");
  }

  /** Set the default spend profile in the User Manager profile. Caller must be SAM admin. */
  public void setDefaultSpendProfile(String email, String spendProfile) {
    setUserProfile(SPEND_PROFILE_PATH, new AnyObject().value(spendProfile), email);
  }

  /**
   * Get the default spend profile in the User Manager profile. Must be SAM admin to supply an
   * email.
   */
  public String getDefaultSpendProfile(@Nullable String email) {
    if (Context.getServer().getUserManagerUri() == null) {
      return Server.DEFAULT_SPEND_PROFILE;
    }

    return ObjectUtils.firstNonNull(
        (String) getUserProfile(SPEND_PROFILE_PATH, email).getValue(),
        Server.DEFAULT_SPEND_PROFILE);
  }

  /**
   * Utility method that checks if an exception thrown by the User client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  private static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    logErrorMessage((ApiException) ex);
    var statusCode = ((ApiException) ex).getCode();
    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
  }

  /** Pull a human-readable error message from an ApiException. */
  private static String logErrorMessage(ApiException ex) {
    logger.error(
        "User Manager exception status code: {}, response body: {}, message: {}",
        ex.getCode(),
        ex.getResponseBody(),
        ex.getMessage());

    // try to deserialize the response body into an ErrorReport
    var responseBody = ex.getResponseBody();

    // if we found a User Manager error message, then return it
    // otherwise return a string with the http code
    return !TextUtils.isEmpty(responseBody) ? responseBody : ex.getCode() + " " + ex.getMessage();
  }

  /**
   * Execute a function that includes hitting User Manager endpoints. Retry if the function throws
   * an {@link #isRetryable} exception. If an exception is thrown by the User client or the retries,
   * make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the User client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, UserManagerService::isRetryable), errorMsg);
  }

  /**
   * Execute a function that includes hitting User Manager endpoints. If an exception is thrown by
   * the User Manager client or the retries, make sure the HTTP status code and error message are
   * logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the User Manager client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is an User Manager client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        errorMsg += ": " + logErrorMessage((ApiException) ex);
      }

      // wrap the User Manager exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }
}
