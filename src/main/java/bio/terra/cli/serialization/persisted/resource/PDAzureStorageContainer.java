package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AzureStorageContainer;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;

/**
 * External representation of a workspace Azure storage container resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.resource.AzureStorageContainer} class for an Azure
 * storage container's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAzureStorageContainer.Builder.class)
public class PDAzureStorageContainer extends PDResource {
  public final UUID storageAccountId;
  public final String storageContainerName;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAzureStorageContainer(AzureStorageContainer internalObj) {
    super(internalObj);
    this.storageAccountId = internalObj.getStorageAccountId();
    this.storageContainerName = internalObj.getStorageContainerName();
  }

  private PDAzureStorageContainer(PDAzureStorageContainer.Builder builder) {
    super(builder);
    this.storageAccountId = builder.storageAccountId;
    this.storageContainerName = builder.storageContainerName;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AzureStorageContainer deserializeToInternal() {
    return new AzureStorageContainer(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private UUID storageAccountId;
    private String storageContainerName;

    /** Default constructor for Jackson. */
    public Builder() {}

    public PDAzureStorageContainer.Builder storageAccountId(UUID storageAccountId) {
      this.storageAccountId = storageAccountId;
      return this;
    }

    public PDAzureStorageContainer.Builder storageContainerName(String storageContainerName) {
      this.storageContainerName = storageContainerName;
      return this;
    }

    /** Call the private constructor. */
    public PDAzureStorageContainer build() {
      return new PDAzureStorageContainer(this);
    }
  }
}
