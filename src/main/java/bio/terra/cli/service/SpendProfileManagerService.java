package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyMembershipRequest;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntryV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to the future Spend Profile Manager service. */
public class SpendProfileManagerService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);
  // these are the resource type and id of the spend profile used by WSM. currently there is
  // only one SAM resource used. in the future, if this varies per environment, move this resource
  // id into the server specification
  private static final String SPEND_PROFILE_RESOURCE_TYPE = "spend-profile";
  // there currently is no SPM service, so this class just wraps calls to SAM
  // keep a reference to the SAM service instance here
  private final SamService samService;
  private final UserManagerService userManagerService;

  private SpendProfileManagerService(SamService samService, UserManagerService userManagerService) {
    this.samService = samService;
    this.userManagerService = userManagerService;
  }

  /**
   * Factory method for class that talks to the SPM service. The user must be authenticated. Methods
   * in this class will use its credentials to call authenticated endpoints. This factory method
   * uses the current context's server and user.
   */
  public static SpendProfileManagerService fromContext() {
    return new SpendProfileManagerService(
        SamService.fromContext(),
        Context.getServer().getUserManagerUri() != null ? UserManagerService.fromContext() : null);
  }

  /**
   * Add the specified email to a spend profile resource in SAM.
   *
   * @param email email of the user or group to add
   * @param spendProfile name of the spend profile resource
   */
  public void enableUserForSpendProfile(
      SpendProfilePolicy policy, String email, String spendProfile, boolean saveToUserProfile) {
    samService.addUserToResourceOrInviteUser(
        SPEND_PROFILE_RESOURCE_TYPE, spendProfile, policy.getSamPolicy(), email);
    if (userManagerService != null && saveToUserProfile) {
      userManagerService.setDefaultSpendProfile(email, spendProfile);
    }
  }

  /**
   * Remove the specified email from a spend profile resource in SAM.
   *
   * @param email email of the user or group to remove
   * @param spendProfile name of the spend profile resource
   */
  public void disableUserForSpendProfile(
      SpendProfilePolicy policy, String email, String spendProfile) {
    samService.removeUserFromResource(
        SPEND_PROFILE_RESOURCE_TYPE, spendProfile, policy.getSamPolicy(), email);
  }

  /**
   * List the members of a spend profile resource in SAM.
   *
   * @param spendProfile name of the spend profile resource
   * @return a list of policies with their member emails
   */
  public List<AccessPolicyResponseEntryV2> listUsersOfSpendProfile(String spendProfile) {
    return samService.listPoliciesForResource(SPEND_PROFILE_RESOURCE_TYPE, spendProfile);
  }

  /** Create a new SAM resource for a spend profile. */
  public void createSpendProfile(String spendProfile) {
    // create two policies (owner, user) and make sure the current user is an owner
    Map<String, AccessPolicyMembershipRequest> policies = new HashMap<>();
    policies.put(
        "owner",
        new AccessPolicyMembershipRequest()
            .addRolesItem("owner")
            .addMemberEmailsItem(Context.requireUser().getEmail()));
    policies.put("user", new AccessPolicyMembershipRequest().addRolesItem("user"));

    samService.createResource(SPEND_PROFILE_RESOURCE_TYPE, spendProfile, policies);
  }

  /** Delete the SAM resource for a spend profile. */
  public void deleteSpendProfile(String spendProfile) {
    samService.deleteResource(SPEND_PROFILE_RESOURCE_TYPE, spendProfile);
  }

  /**
   * These are the policies for the WSM spend profile resource. They can be looked up dynamically by
   * calling the SAM "/api/resources/v1/{resourceTypeName}/{resourceId}/policies" GET endpoint. They
   * are hard-coded here to show the possible values in the CLI usage help. (And in the eventual SPM
   * service, this may be an enum in their API?)
   */
  public enum SpendProfilePolicy {
    OWNER,
    USER;

    /** Helper method to get the SAM string that corresponds to this spend profile policy. */
    public String getSamPolicy() {
      return name().toLowerCase();
    }
  }
}
