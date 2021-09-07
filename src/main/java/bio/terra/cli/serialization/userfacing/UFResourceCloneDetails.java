package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ResourceCloneDetails;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.PrintStream;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

@JsonDeserialize(builder = UFResourceCloneDetails.Builder.class)
public class UFResourceCloneDetails {
  public final CloningInstructionsEnum cloningInstructions;
  public final Resource.Type resourceType;
  public final StewardshipType stewardshipType;
  public final UUID sourceResourceId;
  @Nullable public final UUID destinationResourceId;
  //  public final CloneResourceResult result;
  @Nullable public final String errorMessage;
  @Nullable public final String name;
  @Nullable public final String description;

  public UFResourceCloneDetails(ResourceCloneDetails resourceCloneDetails) {
    this.cloningInstructions = resourceCloneDetails.getCloningInstructions();
    this.resourceType = Resource.fromClientResourceType(resourceCloneDetails.getResourceType());
    this.stewardshipType = resourceCloneDetails.getStewardshipType();
    this.sourceResourceId = resourceCloneDetails.getSourceResourceId();
    this.destinationResourceId = resourceCloneDetails.getDestinationResourceId();
    this.errorMessage = resourceCloneDetails.getErrorMessage();
    this.name = resourceCloneDetails.getName();
    this.description = resourceCloneDetails.getDescription();
  }

  protected UFResourceCloneDetails(Builder builder) {
    this.cloningInstructions = builder.cloningInstructions;
    this.resourceType = builder.resourceType;
    this.stewardshipType = builder.stewardshipType;
    this.sourceResourceId = builder.sourceResourceId;
    this.destinationResourceId = builder.destinationResourceId;
    //    this.result = builder.result;
    this.errorMessage = builder.errorMessage;
    this.name = builder.name;
    this.description = builder.description;
  }

  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("\tName:                     " + Optional.ofNullable(name).orElse(""));
    OUT.println("\tDescription:              " + Optional.ofNullable(description).orElse(""));
    OUT.println("\tStewardship:              " + stewardshipType);
    OUT.println("\tCloning:                  " + cloningInstructions);
    OUT.println("\tSource Resource ID:       " + sourceResourceId.toString());
    OUT.println(
        "\tDestination Resource ID:  "
            + Optional.ofNullable(destinationResourceId).map(UUID::toString).orElse(""));
    //    OUT.println("\tResult:                   " + result);
    OUT.println("\tError Message:            " + Optional.ofNullable(errorMessage).orElse(""));
    OUT.println();
  }

  public abstract static class Builder {
    private CloningInstructionsEnum cloningInstructions;
    private Resource.Type resourceType;
    private StewardshipType stewardshipType;
    private UUID sourceResourceId;
    private UUID destinationResourceId;
    //    private CloneResourceResult result;
    private String errorMessage;
    private String name;
    private String description;

    public Builder cloningInstructions(CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    public Builder resourceType(Resource.Type resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder stewardshipType(StewardshipType stewardshipType) {
      this.stewardshipType = stewardshipType;
      return this;
    }

    public Builder sourceResourceId(UUID sourceResourceId) {
      this.sourceResourceId = sourceResourceId;
      return this;
    }

    public Builder destinationResourceId(UUID destinationResourceId) {
      this.destinationResourceId = destinationResourceId;
      return this;
    }

    //    public Builder result(CloneResourceResult result) {
    //      this.result = result;
    //      return this;
    //    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }
  }
}
