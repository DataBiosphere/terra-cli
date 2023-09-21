package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.HttpClients;
import bio.terra.cli.utils.JacksonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.AdminApi;
import org.broadinstitute.dsde.workbench.client.sam.api.GoogleApi;
import org.broadinstitute.dsde.workbench.client.sam.api.GroupApi;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyMembershipRequest;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntryV2;
import org.broadinstitute.dsde.workbench.client.sam.model.CreateResourceRequestV2;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusDetails;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling SAM endpoints. */
public class SamService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);
  // the Terra environment where the SAM service lives
  private final Server server;
  // access token to use for authenticated requests
  private final AccessToken accessToken;
  // the client object used for talking to SAM
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to SAM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  private SamService(@Nullable AccessToken accessToken, Server server) {
    this.accessToken = accessToken;
    this.server = server;
    this.apiClient = new ApiClient();

    this.apiClient.setHttpClient(HttpClients.getSamClient());
    this.apiClient.setBasePath(server.getSamUri());
    this.apiClient.setUserAgent("OpenAPI-Generator/1.0.0 java"); // only logs an error in sam
    if (accessToken != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
  }

  /**
   * Factory method for class that talks to SAM. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static SamService unauthenticated(Server server) {
    return new SamService(null, server);
  }

  /**
   * Factory method for class that talks to SAM. Pulls the current server and user from the context.
   */
  public static SamService fromContext() {
    return forUser(Context.requireUser());
  }

  /** Factory method for class that talks to SAM. Pulls the current server from the context. */
  public static SamService forUser(User user) {
    return new SamService(user.getTerraToken(), Context.getServer());
  }

  /** Factory method for class that talks to SAM. Pulls the current server from the context. */
  public static SamService forToken(AccessToken accessToken) {
    return new SamService(accessToken, Context.getServer());
  }

  /**
   * Utility method that checks if an exception thrown by the SAM client matches the given HTTP
   * status code.
   *
   * @param ex exception to test
   * @return true if the exception status code matches
   */
  private static boolean isHttpStatusCode(Exception ex, int statusCode) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    int exceptionStatusCode = ((ApiException) ex).getCode();
    return statusCode == exceptionStatusCode;
  }

  /**
   * Utility method that checks if an exception thrown by the SAM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    logErrorMessage((ApiException) ex);
    int statusCode = ((ApiException) ex).getCode();

    // if the SAM client gets a SocketTimeoutException, it wraps it in an ApiException, sets the
    // HTTP status code to 0, and rethrows it to the caller. detect this case here and retry it.
    final int TIMEOUT_STATUS_CODE = 0;
    boolean isSamInternalSocketTimeout =
        statusCode == TIMEOUT_STATUS_CODE && ex.getCause() instanceof SocketTimeoutException;

    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT
        || isSamInternalSocketTimeout;
  }

  /** Pull a human-readable error message from an ApiException. */
  private static String logErrorMessage(ApiException apiEx) {
    logger.error(
        "SAM exception status code: {}, response body: {}, message: {}",
        apiEx.getCode(),
        apiEx.getResponseBody(),
        apiEx.getMessage());

    // try to deserialize the response body into an ErrorReport
    String apiExMsg = apiEx.getResponseBody();
    if (apiExMsg != null)
      try {
        ErrorReport errorReport =
            JacksonMapper.getMapper().readValue(apiEx.getResponseBody(), ErrorReport.class);
        apiExMsg = errorReport.getMessage();
      } catch (JsonProcessingException jsonEx) {
        logger.debug("Error deserializing SAM exception ErrorReport: {}", apiEx.getResponseBody());
      }

    // if we found a SAM error message, then return it
    // otherwise return a string with the http code
    return ((apiExMsg != null && !apiExMsg.isEmpty())
        ? apiExMsg
        : apiEx.getCode() + " " + apiEx.getMessage());
  }

  /**
   * Call the SAM "/status" endpoint to get the status of the server.
   *
   * @return the SAM status object, null if there was an error checking the status
   */
  public SystemStatus getStatus() {
    return callWithRetries(new StatusApi(apiClient)::getSystemStatus, "Error getting SAM status.");
  }

  /**
   * Call the SAM "/register/user/v1" endpoint to register the user who is currently logged in. This
   * is not the same as inviting a user.
   */
  public void registerUser() {
    callWithRetries(
        () -> {
          UserStatus userStatus = new UsersApi(apiClient).createUserV2(/*body=*/ null);
          logger.info(
              "User registered in SAM: {}, {}",
              userStatus.getUserInfo().getUserSubjectId(),
              userStatus.getUserInfo().getUserEmail());
        },
        "Error registering new user in SAM.");
  }

  /**
   * Call the SAM "/api/users/v1/invite/{inviteeEmail}" endpoint to invite a user and track them.
   * This is not the same thing as registering a user.
   *
   * @param userEmail email to invite
   */
  public void inviteUser(String userEmail) {
    callWithRetries(
        () -> {
          logger.info("Inviting new user: {}", userEmail);
          UserStatusDetails userStatusDetails =
              new UsersApi(apiClient).inviteUser(userEmail, /*body=*/ null);
          logger.info("Invited new user: {}", userStatusDetails);
        },
        "Error inviting new user in SAM.");
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * @return SAM object with details about the user
   */
  public UserStatusInfo getUserInfoForSelf() {
    return callWithRetries(
        new UsersApi(apiClient)::getUserStatusInfo,
        "Error reading user information from SAM for current user.");
  }

  /**
   * Call the SAM "/api/admin/user/email/{email}" admin endpoint to get the user info for the
   * specified email.
   *
   * @return SAM object with details about the user, or null if not found
   */
  public UserStatus getUserInfo(String email) {
    return callWithRetries(
        () -> {
          try {
            return new AdminApi(apiClient).adminGetUserByEmail(email);
          } catch (Exception ex) {
            if (isHttpStatusCode(ex, HttpStatusCodes.STATUS_CODE_NOT_FOUND)) {
              return null;
            }
            throw ex;
          }
        },
        "Error reading user information from SAM.");
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * <p>If that returns a Not Found error, then call the SAM "register/user/v2/self" endpoint to
   * register the user.
   *
   * @return SAM object with details about the user
   */
  public UserStatusInfo getUserInfoOrRegisterUser() {
    // - try to lookup the user
    // - if the user lookup failed with a Not Found error, it means the email is not found
    // - so try to register the user first, then retry looking them up
    return callAndHandleOneTimeError(
        new UsersApi(apiClient)::getUserStatusInfo,
        ex -> isHttpStatusCode(ex, HttpStatusCodes.STATUS_CODE_NOT_FOUND),
        this::registerUser,
        "Error reading user information from SAM.");
  }

  /**
   * Call the SAM "/api/google/v1/user/proxyGroup/{email}" endpoint to get the email for the current
   * user's proxy group.
   *
   * @param email address of the user
   * @return email address of the user's proxy group
   */
  public String getProxyGroupEmail(String email) {
    return callWithRetries(
        () -> new GoogleApi(apiClient).getProxyGroup(email),
        "Error getting proxy group email from SAM.");
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}" POST endpoint to create a new group.
   *
   * @param groupName name of the new group
   */
  public void createGroup(String groupName) {
    callWithRetries(
        () -> new GroupApi(apiClient).postGroup(groupName, /*body=*/ null),
        "Error creating SAM group.");
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}" DELETE endpoint to delete an existing group.
   *
   * @param groupName name of the group to delete
   */
  public void deleteGroup(String groupName) {
    callWithRetries(
        () -> new GroupApi(apiClient).deleteGroup(groupName), "Error deleting SAM group.");
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}/{policyName}" GET endpoint to get the email addresses
   * of a group + policy.
   *
   * @param groupName name of the group
   * @param policy policy the users belong to
   * @return a list of users that belong to the group with the specified policy
   */
  public List<String> listUsersInGroup(String groupName, GroupPolicy policy) {
    return callWithRetries(
        () -> new GroupApi(apiClient).getGroupPolicyEmails(groupName, policy.getSamPolicy()),
        "Error listing users in SAM group.");
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}/{policyName}/{email}" PUT endpoint to add an email
   * address to a group + policy.
   *
   * @param groupName name of the group
   * @param policy policy the user will belong to
   * @param userEmail email of the user to add
   */
  public void addUserToGroup(String groupName, GroupPolicy policy, String userEmail) {
    if (server.getSamInviteRequiresAdmin()) {
      // if inviting a user requires admin permissions, don't invite whenever a user is not found
      // instead, require the admin to explicitly invite someone
      callWithRetries(
          () ->
              new GroupApi(apiClient)
                  .addEmailToGroup(groupName, policy.getSamPolicy(), userEmail, /*body=*/ null),
          "Error adding user to SAM group.");
    } else {
      // - try to add the email to the group
      // - if this fails with a Bad Request error, it means the email is not found
      // - so try to invite the user first, then retry adding them to the group
      callAndHandleOneTimeError(
          () ->
              new GroupApi(apiClient)
                  .addEmailToGroup(groupName, policy.getSamPolicy(), userEmail, /*body=*/ null),
          (ex) -> isHttpStatusCode(ex, HttpStatusCodes.STATUS_CODE_BAD_REQUEST),
          () -> inviteUser(userEmail),
          "Error adding user to SAM group.");
    }
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}/{policyName}/{email}" DELETE endpoint to remove an
   * email address from a group + policy.
   *
   * @param groupName name of the group
   * @param policy policy the user belongs to
   * @param userEmail email of the user to remove
   */
  public void removeUserFromGroup(String groupName, GroupPolicy policy, String userEmail) {
    callWithRetries(
        () ->
            new GroupApi(apiClient)
                .removeEmailFromGroup(groupName, policy.getSamPolicy(), userEmail),
        "Error removing user from SAM group.");
  }

  /**
   * Call the SAM "/api/groups/v1" GET endpoint to get the groups to which the current user belongs.
   *
   * @return a list of groups
   */
  public List<ManagedGroupMembershipEntry> listGroups() {
    return callWithRetries(
        new GroupApi(apiClient)::listGroupMemberships, "Error listing SAM groups.");
  }

  /**
   * Call the SAM
   * "/api/resources/v1/{resourceTypeName}/{resourceId}/policies/{policyName}/memberEmails/{email}"
   * PUT endpoint to add an email address to a resource + policy.
   *
   * <p>If that returns a Not Found error, then call the SAM "/api/users/v1/invite/{inviteeEmail}"
   * endpoint to invite the user.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @param resourcePolicyName name of resource policy
   * @param userEmail email of the user or group to add
   */
  public void addUserToResourceOrInviteUser(
      String resourceType, String resourceId, String resourcePolicyName, String userEmail) {
    if (server.getSamInviteRequiresAdmin()) {
      // if inviting a user requires admin permissions, don't invite whenever a user is not found
      // instead, require the admin to explicitly invite someone
      callWithRetries(
          () ->
              new ResourcesApi(apiClient)
                  .addUserToPolicyV2(
                      resourceType, resourceId, resourcePolicyName, userEmail, /*body=*/ null),
          "Error adding user to SAM resource.");
    } else {
      // - try to add user to the policy
      // - if the add user to policy failed with a Bad Request error, it means the email is not
      // found
      // - so try to invite the user first, then retry adding them to the policy
      callAndHandleOneTimeError(
          () ->
              new ResourcesApi(apiClient)
                  .addUserToPolicyV2(
                      resourceType, resourceId, resourcePolicyName, userEmail, /*body=*/ null),
          (ex) -> isHttpStatusCode(ex, HttpStatusCodes.STATUS_CODE_BAD_REQUEST),
          () -> inviteUser(userEmail),
          "Error adding user to SAM resource.");
    }
  }

  /**
   * Call the SAM
   * "/api/resources/v1/{resourceTypeName}/{resourceId}/policies/{policyName}/memberEmails/{email}"
   * DELETE endpoint to remove an email address from a resource + policy.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @param resourcePolicyName name of resource policy
   * @param userEmail email of the user or group to remove
   */
  public void removeUserFromResource(
      String resourceType, String resourceId, String resourcePolicyName, String userEmail) {
    callWithRetries(
        () ->
            new ResourcesApi(apiClient)
                .removeUserFromPolicyV2(resourceType, resourceId, resourcePolicyName, userEmail),
        "Error removing user from SAM resource.");
  }

  /**
   * Call the SAM "/api/resources/v1/{resourceTypeName}/{resourceId}/allUsers" GET endpoint to list
   * all policies of a resource and their members.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @return list of policies on the resource and their members
   */
  public List<AccessPolicyResponseEntryV2> listPoliciesForResource(
      String resourceType, String resourceId) {
    return callWithRetries(
        () -> new ResourcesApi(apiClient).listResourcePoliciesV2(resourceType, resourceId),
        "Error getting policies for SAM resource.");
  }

  /**
   * Call the SAM "/api/resources/v2/{resourceTypeName}" POST endpoint to create a new resource with
   * the given policies (i.e. not default owner policy).
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @param policies list of policies on the resource
   */
  public void createResource(
      String resourceType, String resourceId, Map<String, AccessPolicyMembershipRequest> policies) {
    CreateResourceRequestV2 request =
        new CreateResourceRequestV2().resourceId(resourceId).policies(policies);
    logger.debug("create resource request: {}", request);
    callWithRetries(
        () -> new ResourcesApi(apiClient).createResourceV2(resourceType, request),
        "Error creating SAM resource.");
  }

  /**
   * Call the SAM "/api/resources/v2/{resourceTypeName}/{resourceId}" DELETE endpoint to delete an
   * existing resource.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   */
  public void deleteResource(String resourceType, String resourceId) {
    callWithRetries(
        () -> new ResourcesApi(apiClient).deleteResourceV2(resourceType, resourceId),
        "Error deleting SAM resource.");
  }

  /**
   * Call the SAM "POST /api/google/v1/user/petServiceAccount/{project}" GET endpoint to get a
   * project-specific pet SA email for the current user (i.e. the one whose credentials were
   * supplied to the apiClient object).
   *
   * @param googleProjectId the Google project id
   * @return the pet SA email
   */
  public String getPetSaEmailForProject(String googleProjectId) {
    return callWithRetries(
        () -> new GoogleApi(apiClient).getPetServiceAccount(googleProjectId),
        "Error getting pet SA email for project from SAM.");
  }

  /**
   * Call the SAM "POST /api/google/v1/user/petServiceAccount/{project}/token" GET endpoint to get a
   * project-specific pet SA access token for the current user (i.e. the one whose credentials were
   * supplied to the apiClient object).
   *
   * @param googleProjectId the Google project id
   * @param scopes access scopes
   * @return the access token string
   */
  public String getPetSaAccessTokenForProject(String googleProjectId, List<String> scopes) {
    return callWithRetries(
        () -> new GoogleApi(apiClient).getPetServiceAccountToken(googleProjectId, scopes),
        "Error getting pet SA access token for project from SAM.");
  }

  /**
   * Execute a function that includes hitting SAM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the SAM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   */
  private void callWithRetries(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, SamService::isRetryable), errorMsg);
  }

  /**
   * Execute a function that includes hitting SAM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the SAM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the result of makeRequest
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, SamService::isRetryable), errorMsg);
  }

  /**
   * Execute a function, and possibly a second function to handle a one-time error, that includes
   * hitting SAM endpoints. Retry if the function throws an {@link #isRetryable} exception. If an
   * exception is thrown by the SAM client or the retries, make sure the HTTP status code and error
   * message are logged.
   *
   * @param makeRequest function with no return value
   * @param isOneTimeError function to test whether the exception is the expected one-time error
   * @param handleOneTimeError function to handle the one-time error before retrying the request
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   */
  private void callAndHandleOneTimeError(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest,
      Predicate<Exception> isOneTimeError,
      HttpUtils.RunnableWithCheckedException<ApiException> handleOneTimeError,
      String errorMsg) {
    handleClientExceptions(
        () ->
            HttpUtils.callAndHandleOneTimeErrorWithRetries(
                makeRequest,
                SamService::isRetryable,
                isOneTimeError,
                handleOneTimeError,
                (ex) ->
                    false), // don't retry because the handleOneTimeError already includes retries
        errorMsg);
  }

  /**
   * Execute a function, and possibly a second function to handle a one-time error, that includes
   * hitting SAM endpoints. Retry if the function throws an {@link #isRetryable} exception. If an
   * exception is thrown by the SAM client or the retries, make sure the HTTP status code and error
   * message are logged.
   *
   * @param makeRequest function with a return value
   * @param isOneTimeError function to test whether the exception is the expected one-time error
   * @param handleOneTimeError function to handle the one-time error before retrying the request
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the result of makeRequest
   */
  private <T> T callAndHandleOneTimeError(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest,
      Predicate<Exception> isOneTimeError,
      HttpUtils.RunnableWithCheckedException<ApiException> handleOneTimeError,
      String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callAndHandleOneTimeErrorWithRetries(
                makeRequest,
                SamService::isRetryable,
                isOneTimeError,
                handleOneTimeError,
                (ex) ->
                    false), // don't retry because the handleOneTimeError already includes retries
        errorMsg);
  }

  /**
   * Execute a function that includes hitting SAM endpoints. If an exception is thrown by the SAM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   */
  private void handleClientExceptions(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> {
          makeRequest.run();
          return null;
        },
        errorMsg);
  }

  /**
   * Execute a function that includes hitting SAM endpoints. If an exception is thrown by the SAM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the SAM client or the retries
   * @param <T> type of the response object (i.e. return type of the makeRequest function)
   * @return the result of makeRequest
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is a SAM client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        String exceptionErrorMessage = logErrorMessage((ApiException) ex);

        // if this is a not-invited user access denied error, then throw a more user-friendly error
        // message
        if (exceptionErrorMessage.contains("request an invite from an admin")
            && ((ApiException) ex).getCode() == HttpStatusCodes.STATUS_CODE_BAD_REQUEST) {
          throw new UserActionableException(
              "Fetching the user's registration information failed. Ask an administrator to invite you.",
              ex);
        }

        errorMsg += ": " + exceptionErrorMessage;
      }

      // wrap the SAM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    } finally {
      // try to close the connection pool after we're finished with this request
      // TODO: why is this needed? possibly a bad interaction with picoCLI?
      try {
        apiClient.getHttpClient().connectionPool().evictAll();
      } catch (Exception anyEx) {
        logger.error(
            "Error forcing connection pool to shutdown after making a SAM client library call.",
            anyEx);
      }
    }
  }

  /**
   * Possible values for the policies on a SAM group. These values are defined as an enum in SAM's
   * API YAML
   * (https://github.com/broadinstitute/sam/blob/61135c798873d20a308be1e440b862bf9767c243/src/main/resources/swagger/api-docs.yaml#L383)
   * but I don't see an enum in the client library. It looks like that was a bug in Swagger codegen
   * until v2.1.5 (https://github.com/swagger-api/swagger-codegen/pull/1740), but I'm not sure if
   * that applies to the version that SAM is using.
   */
  public enum GroupPolicy {
    MEMBER,
    ADMIN;

    /** Get the group policy that corresponds to the SAM string. */
    public static GroupPolicy fromSamPolicy(String samPolicyName) {
      return GroupPolicy.valueOf(samPolicyName.toUpperCase());
    }

    /** Get the SAM string that corresponds to this group policy. */
    public String getSamPolicy() {
      return name().toLowerCase();
    }
  }
}
