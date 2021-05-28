package bio.terra.cli.service.utils;

import bio.terra.cli.context.Server;
import bio.terra.cli.context.TerraUser;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to the future Spend Profile Manager service. */
public class SpendProfileManagerService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  // the Terra environment where the SPM service lives
  private final Server server;

  // the Terra user whose credentials will be used to call authenticated requests
  private final TerraUser terraUser;

  // there currently is no SPM service, so this class just wraps calls to SAM
  // keep a reference to the SAM service instance here
  private final SamService samService;

  // these are the resource type and id of the default spend profile used by WSM. currently there is
  // only one SAM resource used. in the future, if this varies per environment, move this resource
  // id into the server specification
  private static final String SPEND_PROFILE_RESOURCE_TYPE = "spend-profile";
  private static final String WSM_DEFAULT_SPEND_PROFILE_RESOURCE_ID = "wm-default-spend-profile";

  /**
   * Constructor for class that talks to the SPM service. The user must be authenticated. Methods in
   * this class will use its credentials to call authenticated endpoints.
   *
   * @param server the Terra environment where the SAM service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  public SpendProfileManagerService(Server server, TerraUser terraUser) {
    this.server = server;
    this.terraUser = terraUser;
    this.samService = new SamService(server, terraUser);
  }

  /**
   * These are the policies for the WSM default spend profile resource. They can be looked up
   * dynamically by calling the SAM "/api/resources/v1/{resourceTypeName}/{resourceId}/policies" GET
   * endpoint. They are hard-coded here to show the possible values in the CLI usage help. (And in
   * the eventual SPM service, this may be an enum in their API?)
   */
  public enum SpendProfilePolicy {
    owner,
    user
  }

  /**
   * Add the specified email to the WSM default spend profile resource in SAM.
   *
   * @param email email of the user or group to add
   */
  public void enableUserForDefaultSpendProfile(SpendProfilePolicy policy, String email) {
    samService.addUserToResourceOrInviteUser(
        SPEND_PROFILE_RESOURCE_TYPE, WSM_DEFAULT_SPEND_PROFILE_RESOURCE_ID, policy.name(), email);
  }

  /**
   * Remove the specified email from the WSM default spend profile resource in SAM.
   *
   * @param email email of the user or group to remove
   */
  public void disableUserForDefaultSpendProfile(SpendProfilePolicy policy, String email) {
    samService.removeUserFromResource(
        SPEND_PROFILE_RESOURCE_TYPE, WSM_DEFAULT_SPEND_PROFILE_RESOURCE_ID, policy.name(), email);
  }

  /**
   * List the members of the WSM default spend profile resource in SAM.
   *
   * @return a list of policies with their member emails
   */
  public List<AccessPolicyResponseEntry> listUsersOfDefaultSpendProfile() {
    return samService.listPoliciesForResource(
        SPEND_PROFILE_RESOURCE_TYPE, WSM_DEFAULT_SPEND_PROFILE_RESOURCE_ID);
  }
}
