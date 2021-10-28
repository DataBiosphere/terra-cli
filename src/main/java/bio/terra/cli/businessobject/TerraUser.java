package bio.terra.cli.businessobject;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.SamService;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of someone who could register as a user in Terra. This is different from
 * a regular {@link User} because they are never logged in. This is just a reference to another
 * Terra user who has a record in SAM. This class is not part of the current context or state.
 */
public class TerraUser {
  private static final Logger logger = LoggerFactory.getLogger(TerraUser.class);

  private String email;
  private String subjectId;
  private boolean isRegistered;
  private boolean isEnabled;

  private TerraUser(String email, String subjectId, boolean isRegistered, boolean isEnabled) {
    this.email = email;
    this.subjectId = subjectId;
    this.isRegistered = isRegistered;
    this.isEnabled = isEnabled;
  }

  /**
   * Invite a user to Terra.
   *
   * @param email email of the user to invite
   */
  public static void invite(String email) {
    SamService.fromContext().inviteUser(email);
    logger.info("Invited user in SAM: email={}", email);
  }

  /** Get the registered user object. */
  public static TerraUser getUser(String email) {
    UserStatus userStatus = SamService.fromContext().getUserInfo(email);
    if (userStatus == null) {
      throw new UserActionableException("User not found: " + email);
    }
    logger.info("Found user in SAM: {}", userStatus);

    // lowercase the email so there is a consistent way of looking up the email address
    // the email address casing in SAM may not match the case of what is provided by the user
    String emailFromSAM = userStatus.getUserInfo().getUserEmail().toLowerCase();
    String subjectIdFromSAM = userStatus.getUserInfo().getUserSubjectId();

    // user is in the all-users group, enabled, and is in their google proxy group
    boolean isEnabledFromSAM = userStatus.getEnabled().getLdap();
    boolean isRegisteredFromSAM =
        userStatus.getEnabled().getAllUsersGroup() && userStatus.getEnabled().getGoogle();
    return new TerraUser(emailFromSAM, subjectIdFromSAM, isRegisteredFromSAM, isEnabledFromSAM);
  }

  // ====================================================
  // Property getters.

  public String getEmail() {
    return email;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public boolean isRegistered() {
    return isRegistered;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
