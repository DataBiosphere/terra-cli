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
public class UFGroup {
  public final String name;
  public final String email;
  public final Integer numMembers;
  public final List<GroupPolicy> currentUserPolicies;

  public UFGroup(Group internalObj) {
    this.name = internalObj.getName();
    this.email = internalObj.getEmail();
    this.currentUserPolicies = internalObj.getCurrentUserPolicies();

    Integer numMembersNotFinal;
    try {
      numMembersNotFinal = internalObj.getMembers().size();
    } catch (Exception ex) {
      // an exception will be thrown if the user does not have permission to list the group members.
      // only group admins have this permission, so non-admins can see/describe the group, but they
      // can't see how many people are in it.
      numMembersNotFinal = null;
    }
    this.numMembers = numMembersNotFinal;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGroup(Builder builder) {
    this.name = builder.name;
    this.email = builder.email;
    this.numMembers = builder.numMembers;
    this.currentUserPolicies = builder.currentUserPolicies;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    List<String> policiesStr =
        UserIO.sortAndMap(
            currentUserPolicies, Comparator.comparing(GroupPolicy::name), GroupPolicy::toString);
    OUT.println(name);
    OUT.println("  Email: " + email);
    OUT.println("  # Members: " + (numMembers == null ? "(unknown)" : numMembers));
    OUT.println("  Current user's policies: " + String.join(", ", policiesStr));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String email;
    private Integer numMembers;
    private List<GroupPolicy> currentUserPolicies;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder numMembers(Integer numMembers) {
      this.numMembers = numMembers;
      return this;
    }

    public Builder currentUserPolicies(List<GroupPolicy> currentUserPolicies) {
      this.currentUserPolicies = currentUserPolicies;
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
