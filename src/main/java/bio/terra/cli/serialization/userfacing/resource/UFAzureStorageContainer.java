package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AzureStorageContainer;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.UUID;

/**
 * External representation of a workspace Azure storage container resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.resource.AzureStorageContainer} class for an Azure
 * storage container's internal representation.
 */
@JsonDeserialize(builder = UFAzureStorageContainer.Builder.class)
public class UFAzureStorageContainer extends UFResource {
  public final UUID storageAccountId;
  public final String storageContainerName;

  /** Serialize an instance of the internal class to the command format. */
  public UFAzureStorageContainer(AzureStorageContainer internalObj) {
    super(internalObj);
    this.storageAccountId = internalObj.getStorageAccountId();
    this.storageContainerName = internalObj.getStorageContainerName();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAzureStorageContainer(UFAzureStorageContainer.Builder builder) {
    super(builder);
    this.storageAccountId = builder.storageAccountId;
    this.storageContainerName = builder.storageContainerName;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Azure storage account id: " + storageAccountId.toString());
    OUT.println(prefix + "Azure storage container name: " + storageContainerName);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private UUID storageAccountId;
    private String storageContainerName;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UFAzureStorageContainer.Builder storageAccountId(UUID storageAccountId) {
      this.storageAccountId = storageAccountId;
      return this;
    }

    public UFAzureStorageContainer.Builder storageContainerName(String storageContainerName) {
      this.storageContainerName = storageContainerName;
      return this;
    }

    /** Call the private constructor. */
    public UFAzureStorageContainer build() {
      return new UFAzureStorageContainer(this);
    }
  }
}
