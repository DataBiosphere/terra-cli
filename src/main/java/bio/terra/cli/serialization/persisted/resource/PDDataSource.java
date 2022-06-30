package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;

/**
 * External representation of a reference to a data source.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link DataSource} class for a data source's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDDataSource.Builder.class)
public class PDDataSource extends PDResource {
  public final UUID dataSourceWorkspaceUuid;

  public PDDataSource(DataSource internalObj) {
    super(internalObj);
    this.dataSourceWorkspaceUuid = internalObj.getDataSourceWorkspaceUuid();
  }

  private PDDataSource(PDDataSource.Builder builder) {
    super(builder);
    this.dataSourceWorkspaceUuid = builder.dataSourceWorkspaceUuid;
  }

  @Override
  public DataSource deserializeToInternal() {
    return new DataSource(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private UUID dataSourceWorkspaceUuid;

    public PDDataSource.Builder dataSourceWorkspaceUuid(UUID dataSourceWorkspaceUuid) {
      this.dataSourceWorkspaceUuid = dataSourceWorkspaceUuid;
      return this;
    }

    /** Call the private constructor. */
    public PDDataSource build() {
      return new PDDataSource(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
