package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.service.SpendProfileManagerService.SpendProfilePolicy;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

/**
 * External representation of a spend profile user (i.e. someone who has permission to spend money
 * in Terra) for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.SpendProfileUser} class for a spend profile user's
 * internal representation.
 */
@JsonDeserialize(builder = UFSpendProfileUser.Builder.class)
public class UFSpendProfileUser implements UserFacing {
  public final String email;
  public final List<SpendProfilePolicy> policies;

  public UFSpendProfileUser(SpendProfileUser internalObj) {
    this.email = internalObj.getEmail();
    this.policies = internalObj.getPolicies();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFSpendProfileUser(Builder builder) {
    this.email = builder.email;
    this.policies = builder.policies;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    PrintStream OUT = UserIO.getOut();
    List<String> policiesStr =
        UserIO.sortAndMap(
            policies, Comparator.comparing(SpendProfilePolicy::name), SpendProfilePolicy::toString);
    OUT.println(email + ": " + String.join(", ", policiesStr));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String email;
    private List<SpendProfilePolicy> policies;

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder policies(List<SpendProfilePolicy> policies) {
      this.policies = policies;
      return this;
    }

    /** Call the private constructor. */
    public UFSpendProfileUser build() {
      return new UFSpendProfileUser(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
