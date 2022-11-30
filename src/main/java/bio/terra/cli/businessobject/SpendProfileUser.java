package bio.terra.cli.businessobject;

import bio.terra.cli.service.SpendProfileManagerService;
import bio.terra.cli.service.SpendProfileManagerService.SpendProfilePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a spend profile user (i.e. someone who has permission to spend money
 * in Terra). This is different from a regular {@link User} because they are never logged in. This
 * is just a reference to another Terra user who has some permissions on a spend profile. This class
 * is not part of the current context or state.
 */
public class SpendProfileUser {
  private static final Logger logger = LoggerFactory.getLogger(SpendProfileUser.class);
  private String email;
  private List<SpendProfilePolicy> policies;

  private SpendProfileUser(String email, List<SpendProfilePolicy> policies) {
    this.email = email;
    this.policies = policies;
  }

  /**
   * Enable a user on the default WSM spend profile.
   *
   * @param email email of the user to add
   * @param policy policy to assign the user
   */
  public static SpendProfileUser enable(String email, SpendProfilePolicy policy) {
    // call SAM to add a policy + email to the WSM default spend profile resource
    SpendProfileManagerService.fromContext().enableUserForDefaultSpendProfile(policy, email);
    logger.info("Enabled user on spend profile: email={}, policy={}", email, policy);

    // return a SpendProfileUser = email + all policies (not just the one that was added here)
    return getUser(email);
  }

  /**
   * Disable a user on the default WSM spend profile.
   *
   * @param email email of the user to remove
   * @param policy policy to remove from the user
   */
  public static SpendProfileUser disable(String email, SpendProfilePolicy policy) {
    // call SAM to remove a policy + email from the WSM default spend profile resource
    SpendProfileManagerService.fromContext().disableUserForDefaultSpendProfile(policy, email);
    logger.info("Disabled user on spend profile: email={}, policy={}", email, policy);

    // return a SpendProfileUser = email + all policies (not just the one that was removed here)
    return getUser(email);
  }

  /** Get the spend profile user object. */
  private static SpendProfileUser getUser(String email) {
    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    return listUsersInMap().get(email.toLowerCase());
  }

  /**
   * List the users of the default WSM spend profile.
   *
   * @return a list of spend profile users
   */
  public static List<SpendProfileUser> list() {
    return listUsersInMap().values().stream().collect(Collectors.toList());
  }

  /**
   * Get the users of the default WSM spend profile in a map, to make it easy to lookup a particular
   * user.
   *
   * @return a map of email -> spend profile user object
   */
  private static Map<String, SpendProfileUser> listUsersInMap() {
    // call SAM to get the users + policies for the WSM default spend profile resource
    List<AccessPolicyResponseEntry> accessPolicies =
        SpendProfileManagerService.fromContext().listUsersOfDefaultSpendProfile();

    // convert the SAM objects (policy -> list of emails) to CLI objects (email -> list of policies)
    Map<String, SpendProfileUser> spendProfileUsers = new HashMap<>();
    accessPolicies.forEach(
        accessPolicy -> {
          SpendProfilePolicy spendPolicy =
              SpendProfilePolicy.valueOf(accessPolicy.getPolicyName().toUpperCase());
          for (String email : accessPolicy.getPolicy().getMemberEmails()) {
            // lowercase the email so there is a consistent way of looking up the email address
            // the email address casing in SAM may not match the case of what is provided by the
            // user
            String emailLowercase = email.toLowerCase();
            SpendProfileUser spendProfileUser = spendProfileUsers.get(emailLowercase);
            if (spendProfileUser == null) {
              spendProfileUser = new SpendProfileUser(emailLowercase, new ArrayList<>());
              spendProfileUsers.put(emailLowercase, spendProfileUser);
            }
            spendProfileUser.policies.add(spendPolicy);
          }
        });
    return spendProfileUsers;
  }

  // ====================================================
  // Property getters.

  public String getEmail() {
    return email;
  }

  public List<SpendProfilePolicy> getPolicies() {
    return policies;
  }
}
