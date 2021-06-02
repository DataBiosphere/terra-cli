package bio.terra.cli.serialization.command.createupdate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateUpdateGcsBucket.Builder.class)
public class CreateUpdateBqDataset extends CreateUpdateResource {
  public final String projectId;
  public final String datasetId;
  public final String location;

  protected CreateUpdateBqDataset(CreateUpdateBqDataset.Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.location = builder.location;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends CreateUpdateResource.Builder {
    private String projectId;
    private String datasetId;
    private String location;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public CreateUpdateBqDataset build() {
      return new CreateUpdateBqDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
