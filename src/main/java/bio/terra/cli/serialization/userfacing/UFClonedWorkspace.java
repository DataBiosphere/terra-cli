package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.ClonedWorkspace;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UFClonedWorkspace {
  public final UUID sourceWorkspaceId;
  public final UUID destinationWorkspaceId;
  public final List<UFResourceCloneDetails> resources;

  public UFClonedWorkspace(ClonedWorkspace clonedWorkspace) {
    this.sourceWorkspaceId = clonedWorkspace.getSourceWorkspaceId();
    this.destinationWorkspaceId = clonedWorkspace.getDestinationWorkspaceId();
    this.resources = clonedWorkspace.getResources().stream()
        .map(UFResourceCloneDetails::new)
        .collect(Collectors.toList());
  }

  protected UFClonedWorkspace(Builder builder) {
    this.sourceWorkspaceId = builder.sourceWorkspaceId;
    this.destinationWorkspaceId = builder.destinationWorkspaceId;
    this.resources = builder.resources;
  }

  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("Source Workspace ID:       " + sourceWorkspaceId.toString());
    OUT.println("Destination Workspace ID:  " + destinationWorkspaceId.toString());
    OUT.println("Resources:                 ");
    resources.forEach(UFResourceCloneDetails::print);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public abstract static class Builder {
    private UUID sourceWorkspaceId;
    private UUID destinationWorkspaceId;
    private List<UFResourceCloneDetails> resources;

    public Builder sourceWorkspaceId(UUID sourceWorkspaceId) {
      this.sourceWorkspaceId = sourceWorkspaceId;
      return this;
    }

    public Builder destinationWorkspaceId(UUID destinationWorkspaceId) {
      this.destinationWorkspaceId = destinationWorkspaceId;
      return this;
    }

    public Builder resources(List<UFResourceCloneDetails> resources) {
      this.resources = resources;
      return this;
    }

    public abstract UFClonedWorkspace build();

    /** Default constructor for Jackson */
    public Builder() {}
  }
}
