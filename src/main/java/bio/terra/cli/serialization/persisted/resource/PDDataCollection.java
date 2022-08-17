package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;

/**
 * External representation of a reference to a data collection.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link DataCollection} class for a data collection's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDDataCollection.Builder.class)
public class PDDataCollection extends PDResource {
  public final UUID dataCollectionWorkspaceUuid;

  public PDDataCollection(DataCollection internalObj) {
    super(internalObj);
    this.dataCollectionWorkspaceUuid = internalObj.getDataCollectionWorkspaceUuid();
  }

  private PDDataCollection(PDDataCollection.Builder builder) {
    super(builder);
    this.dataCollectionWorkspaceUuid = builder.dataCollectionWorkspaceUuid;
  }

  @Override
  public DataCollection deserializeToInternal() {
    return new DataCollection(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private UUID dataCollectionWorkspaceUuid;

    /** Default constructor for Jackson. */
    public Builder() {}

    public PDDataCollection.Builder dataCollectionWorkspaceUuid(UUID dataCollectionWorkspaceUuid) {
      this.dataCollectionWorkspaceUuid = dataCollectionWorkspaceUuid;
      return this;
    }

    /** Call the private constructor. */
    public PDDataCollection build() {
      return new PDDataCollection(this);
    }
  }
}
