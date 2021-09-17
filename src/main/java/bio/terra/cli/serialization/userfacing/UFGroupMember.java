package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.service.SamService.GroupPolicy;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

/**
 * External representation of a group member (i.e. someone who is a member of a SAM group) for
 * command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.Group.Member} class for a group member's internal
 * representation.
 */
@JsonDeserialize(builder = UFGroupMember.Builder.class)
public class UFGroupMember implements UserFacing {
  public final String email;
  public final List<GroupPolicy> policies;

  public UFGroupMember(Group.Member internalObj) {
    this.email = internalObj.getEmail();
    this.policies = internalObj.getPolicies();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGroupMember(Builder builder) {
    this.email = builder.email;
    this.policies = builder.policies;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    List<String> policiesStr =
        UserIO.sortAndMap(policies, Comparator.comparing(GroupPolicy::name), GroupPolicy::toString);
    OUT.println(email + ": " + String.join(", ", policiesStr));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String email;
    private List<GroupPolicy> policies;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder policies(List<GroupPolicy> policies) {
      this.policies = policies;
      return this;
    }

    /** Call the private constructor. */
    public UFGroupMember build() {
      return new UFGroupMember(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
