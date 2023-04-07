package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Workspace} class for a workspace's internal representation.
 *
 * <p>
 */
@JsonDeserialize(builder = UFWorkspace.Builder.class)
public class UFWorkspace extends UFWorkspaceLight {
  public final long numResources;

  /**
   * Serialize an instance of the internal class to the disk format. Note that the Workspace object
   * should have its resources populated in order for numResources to be correctly determined.
   */
  public UFWorkspace(Workspace internalObj) {
    super(internalObj);
    this.numResources = internalObj.listResources().size();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspace(Builder builder) {
    super(builder);
    this.numResources = builder.numResources;
  }

  /** Print out a workspace object in text format. */
  @Override
  public void print() {
    super.print();
    PrintStream OUT = UserIO.getOut();
    // The space add for readable format because only used with terra workspace describe
    OUT.println("# Resources:       " + numResources);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFWorkspaceLight.Builder {
    private long numResources;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder numResources(long numResources) {
      this.numResources = numResources;
      return this;
    }

    /** Call the private constructor. */
    public UFWorkspace build() {
      return new UFWorkspace(this);
    }
  }
}
