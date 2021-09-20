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
 * External representation of a SAM group for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.Group} class for a group's internal
 * representation.
 */
@JsonDeserialize(builder = UFGroup.Builder.class)
public class UFGroup implements UserFacingPrintable {
  public final String name;
  public final String email;
  public final List<GroupPolicy> currentUserPolicies;
  public final long memberCount;

  public UFGroup(Group internalObj) {
    this.name = internalObj.getName();
    this.email = internalObj.getEmail();
    this.currentUserPolicies = internalObj.getCurrentUserPolicies();
    this.memberCount = internalObj.getMembers().size();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGroup(Builder builder) {
    this.name = builder.name;
    this.email = builder.email;
    this.currentUserPolicies = builder.currentUserPolicies;
    this.memberCount = builder.memberCount;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    PrintStream OUT = UserIO.getOut();
    List<String> policiesStr =
        UserIO.sortAndMap(
            currentUserPolicies, Comparator.comparing(GroupPolicy::name), GroupPolicy::toString);
    OUT.println(name);
    OUT.println("  Email: " + email);
    OUT.println("  Current user's policies: " + String.join(", ", policiesStr));
    OUT.println("  Member count: " + memberCount);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String email;
    private List<GroupPolicy> currentUserPolicies;
    private long memberCount;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder currentUserPolicies(List<GroupPolicy> currentUserPolicies) {
      this.currentUserPolicies = currentUserPolicies;
      return this;
    }

    public Builder memberCount(long memberCount) {
      this.memberCount = memberCount;
      return this;
    }

    /** Call the private constructor. */
    public UFGroup build() {
      return new UFGroup(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
