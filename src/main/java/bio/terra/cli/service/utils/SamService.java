package bio.terra.cli.service.utils;

import bio.terra.cli.command.exception.InternalErrorException;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.GoogleApi;
import org.broadinstitute.dsde.workbench.client.sam.api.GroupApi;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntry;
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
    try {
      return HttpUtils.callWithRetries(() -> statusApi.getSystemStatus(), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error getting SAM status.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/users/v1/invite/{inviteeEmail}" endpoint to invite a user and track them.
   * This is not the same thing as registering a user.
   *
   * @param userEmail email to invite
   * @return SAM object with details about the user
   */
  public UserStatusDetails inviteUser(String userEmail) {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> samUsersApi.inviteUser(userEmail), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error inviting user in SAM.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/register/user/v2/self/info" endpoint to get the user info for the current user
   * (i.e. the one whose credentials were supplied to the apiClient object).
   *
   * @return SAM object with details about the user
   */
  public UserStatusInfo getUserInfo() {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> samUsersApi.getUserStatusInfo(), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error reading user information from SAM.", ex);
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
   * @return SAM object with details about the user
   */
  public UserStatusInfo getUserInfoOrRegisterUser() {
    UsersApi samUsersApi = new UsersApi(apiClient);
    try {
      // first try to lookup the user
      return HttpUtils.callWithRetries(
          () -> samUsersApi.getUserStatusInfo(), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      if (!(ex instanceof ApiException)
          || (((ApiException) ex).getCode() != HttpStatusCodes.STATUS_CODE_NOT_FOUND)) {
        throw new InternalErrorException("Error reading user information from SAM.", ex);
      }
      logger.info("User not found in SAM. Trying to register a new user.");

      try {
        // lookup failed with Not Found error, now try to register the user and look them up again
        UserStatus userStatus =
            HttpUtils.callWithRetries(() -> samUsersApi.createUserV2(), SamService::isRetryable);
        logger.info(
            "User registered in SAM: {}, {}",
            userStatus.getUserInfo().getUserSubjectId(),
            userStatus.getUserInfo().getUserEmail());
        return HttpUtils.callWithRetries(
            () -> samUsersApi.getUserStatusInfo(), SamService::isRetryable);
      } catch (ApiException | InterruptedException secondEx) {
        throw new InternalErrorException("Error reading user information from SAM.", secondEx);
      }
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/google/v1/user/proxyGroup/{email}" endpoint to get the email for the current
   * user's proxy group.
   *
   * @return email address of the user's proxy group
   */
  public String getProxyGroupEmail() {
    GoogleApi googleApi = new GoogleApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> googleApi.getProxyGroup(terraUser.terraUserEmail), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error getting proxy group email from SAM.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}" POST endpoint to create a new group.
   *
   * @param groupName name of the new group
   */
  public void createGroup(String groupName) {
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            groupApi.postGroup(groupName);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error creating SAM group.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}" DELETE endpoint to delete an existing group.
   *
   * @param groupName name of the group to delete
   */
  public void deleteGroup(String groupName) {
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            groupApi.deleteGroup(groupName);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error deleting SAM group.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/groups/v1/{groupName}" GET endpoint to get the email address of a group.
   *
   * @param groupName name of the group to delete
   * @return email address of the group
   */
  public String getGroupEmail(String groupName) {
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      return HttpUtils.callWithRetries(() -> groupApi.getGroup(groupName), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error getting email address of SAM group.", ex);
    } finally {
      closeConnectionPool();
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
    member,
    admin
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
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> groupApi.getGroupAdminEmails(groupName, policy.name()), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error listing users in SAM group.", ex);
    } finally {
      closeConnectionPool();
    }
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
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            groupApi.addEmailToGroup(groupName, policy.name(), userEmail);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error adding user to SAM group.", ex);
    } finally {
      closeConnectionPool();
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
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            groupApi.removeEmailFromGroup(groupName, policy.name(), userEmail);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error removing user from SAM group.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/groups/v1" GET endpoint to get the groups to which the current user belongs.
   *
   * @return a list of groups
   */
  public List<ManagedGroupMembershipEntry> listGroups() {
    GroupApi groupApi = new GroupApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> groupApi.listGroupMemberships(), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error listing users in SAM group.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM
   * "/api/resources/v1/{resourceTypeName}/{resourceId}/policies/{policyName}/memberEmails/{email}"
   * PUT endpoint to add an email address to a resource + policy.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @param resourcePolicyName name of resource policy
   * @param userEmail email of the user or group to add
   */
  public void addUserToResource(
      String resourceType, String resourceId, String resourcePolicyName, String userEmail) {
    ResourcesApi resourcesApi = new ResourcesApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            resourcesApi.addUserToPolicy(resourceType, resourceId, resourcePolicyName, userEmail);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error adding user to SAM resource.", ex);
    } finally {
      closeConnectionPool();
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
    ResourcesApi resourcesApi = new ResourcesApi(apiClient);
    try {
      HttpUtils.callWithRetries(
          () -> {
            resourcesApi.removeUserFromPolicy(
                resourceType, resourceId, resourcePolicyName, userEmail);
            return null;
          },
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error removing user from SAM resource.", ex);
    } finally {
      closeConnectionPool();
    }
  }

  /**
   * Call the SAM "/api/resources/v1/{resourceTypeName}/{resourceId}/allUsers" GET endpoint to list
   * all policies of a resource and their members.
   *
   * @param resourceType type of resource
   * @param resourceId id of resource
   * @return list of policies on the resource and their members
   */
  public List<AccessPolicyResponseEntry> listPoliciesForResource(
      String resourceType, String resourceId) {
    ResourcesApi resourcesApi = new ResourcesApi(apiClient);
    try {
      return HttpUtils.callWithRetries(
          () -> resourcesApi.listResourcePolicies(resourceType, resourceId),
          SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error getting policies for SAM resource.", ex);
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
      logger.error(
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
  public HttpUtils.HttpResponse getPetSaKeyForProject(WorkspaceContext workspaceContext) {
    try {
      return HttpUtils.callWithRetries(
          () -> getPetSaKeyForProjectApiClientWrapper(workspaceContext), SamService::isRetryable);
    } catch (ApiException | InterruptedException ex) {
      throw new InternalErrorException("Error fetching the pet SA key file from SAM.", ex);
    }
  }

  /**
   * Helper method for getting the pet SA key for the current user. This method wraps a raw HTTP
   * request and throws an ApiException on error, mimicing the behavior of the client library.
   *
   * @param workspaceContext
   * @return the HTTP response to the SAM request
   * @throws ApiException if the HTTP status code is not successful
   */
  private HttpUtils.HttpResponse getPetSaKeyForProjectApiClientWrapper(
      WorkspaceContext workspaceContext) throws ApiException {
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
    int statusCode;
    try {
      HttpUtils.HttpResponse response =
          HttpUtils.sendHttpRequest(apiEndpoint, "GET", userAccessToken, null);
      if (HttpStatusCodes.isSuccess(response.statusCode)) {
        return response;
      }
      statusCode = response.statusCode;
    } catch (IOException ioEx) {
      statusCode = HttpStatusCodes.STATUS_CODE_SERVICE_UNAVAILABLE;
    }

    // mimic the SAM client library and throw an ApiException if the status code was not successful
    throw new ApiException(
        statusCode, "Error calling /api/google/v1/user/petServiceAccount/{project}/key endpoint");
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
