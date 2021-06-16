package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.service.SamService.GroupPolicy;
import bio.terra.cli.utils.Printer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

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
  public final List<GroupPolicy> currentUserPolicies;

  public UFGroup(Group internalObj) {
    this.name = internalObj.getName();
    this.email = internalObj.getEmail();
    this.currentUserPolicies = internalObj.getCurrentUserPolicies();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGroup(Builder builder) {
    this.name = builder.name;
    this.email = builder.email;
    this.currentUserPolicies = builder.currentUserPolicies;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = Printer.getOut();
    List<String> policiesStr =
        currentUserPolicies.stream().map(GroupPolicy::toString).collect(Collectors.toList());
    OUT.println(name + ": " + String.join(",", policiesStr));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String email;
    private List<GroupPolicy> currentUserPolicies;

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

    /** Call the private constructor. */
    public UFGroup build() {
      return new UFGroup(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
