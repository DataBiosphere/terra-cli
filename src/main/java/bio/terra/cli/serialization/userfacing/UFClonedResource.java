package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.CloneResourceResult;
import bio.terra.workspace.model.ResourceCloneDetails;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import javax.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

/**
 * User-facing view of cloned resource descriptions.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFClonedResource.Builder.class)
public class UFClonedResource {
  public final CloneResourceResult result;
  public final UFResource sourceResource;
  @Nullable public final UFResource destinationResource;
  @Nullable public final String errorMessage;

  public UFClonedResource(
      ResourceCloneDetails resourceCloneDetails,
      UFResource sourceResource,
      @Nullable UFResource destinationResource) {
    this.result = resourceCloneDetails.getResult();
    this.sourceResource = sourceResource;
    this.destinationResource = destinationResource;
    // JSON blocks in the error message are HTML escaped twice, so unescape them twice.
    this.errorMessage =
        StringEscapeUtils.unescapeHtml4(
            StringEscapeUtils.unescapeHtml4(resourceCloneDetails.getErrorMessage()));
  }

  protected UFClonedResource(Builder builder) {
    this.result = builder.result;
    this.sourceResource = builder.sourceResource;
    this.destinationResource = builder.destinationResource;
    this.errorMessage = builder.errorMessage;
  }

  public void print() {
    final String indent = "    ";
    PrintStream OUT = UserIO.getOut();
    OUT.println("Source resource:");
    sourceResource.print(indent);
    OUT.println();
    if (null != destinationResource) {
      OUT.println("Destination resource:");
      destinationResource.print(indent);
    }
    OUT.println();
    OUT.println("Result: " + result);

    if (null != errorMessage) {
      OUT.println("Error Message: " + errorMessage);
    }
    OUT.println();
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CloneResourceResult result;
    private UFResource sourceResource;
    private UFResource destinationResource;
    private String errorMessage;

    public Builder() {}

    public Builder result(CloneResourceResult result) {
      this.result = result;
      return this;
    }

    public Builder sourceResource(UFResource sourceResource) {
      this.sourceResource = sourceResource;
      return this;
    }

    public Builder destinationResource(UFResource destinationResource) {
      this.destinationResource = destinationResource;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public UFClonedResource build() {
      return new UFClonedResource(this);
    }
  }
}
