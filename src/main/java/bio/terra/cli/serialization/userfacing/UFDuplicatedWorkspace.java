package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;

/**
 * External representation of workspace clone result.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFDuplicatedWorkspace.Builder.class)
public class UFDuplicatedWorkspace {
  public final UFWorkspace sourceWorkspace;
  public final UFWorkspace destinationWorkspace;
  public final List<UFDuplicatedResource> resources;

  public UFDuplicatedWorkspace(
      UFWorkspace sourceWorkspace,
      UFWorkspace destinationWorkspace,
      List<UFDuplicatedResource> clonedResources) {
    this.sourceWorkspace = sourceWorkspace;
    this.destinationWorkspace = destinationWorkspace;
    this.resources = clonedResources;
  }

  protected UFDuplicatedWorkspace(Builder builder) {
    this.sourceWorkspace = builder.sourceWorkspace;
    this.destinationWorkspace = builder.destinationWorkspace;
    this.resources = builder.resources;
  }

  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("Source Workspace:");
    sourceWorkspace.print();
    OUT.println();
    OUT.println("Destination Workspace:");
    destinationWorkspace.print();
    OUT.println();
    OUT.println("Resources:");
    resources.forEach(
        resource -> {
          OUT.println("--------------------------------");
          resource.print();
        });
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UFWorkspace sourceWorkspace;
    private UFWorkspace destinationWorkspace;
    private List<UFDuplicatedResource> resources;

    /** Default constructor for Jackson */
    public Builder() {}

    public Builder sourceWorkspace(UFWorkspace sourceWorkspace) {
      this.sourceWorkspace = sourceWorkspace;
      return this;
    }

    public Builder destinationWorkspace(UFWorkspace destinationWorkspace) {
      this.destinationWorkspace = destinationWorkspace;
      return this;
    }

    public Builder resources(List<UFDuplicatedResource> resources) {
      this.resources = resources;
      return this;
    }

    public UFDuplicatedWorkspace build() {
      return new UFDuplicatedWorkspace(this);
    }
  }
}
