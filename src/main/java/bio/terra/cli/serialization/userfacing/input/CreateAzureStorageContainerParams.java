package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;

/**
 * Parameters for creating an Azure storage container workspace resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateGcsBucketParams.Builder.class)
public class CreateAzureStorageContainerParams {
  public final CreateResourceParams resourceFields;
  public final String storageContainerName;
  public final UUID storageAccountId;

  protected CreateAzureStorageContainerParams(CreateAzureStorageContainerParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.storageContainerName = builder.storageContainerName;
    this.storageAccountId = builder.storageAccountId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String storageContainerName;
    private UUID storageAccountId;

    /** Default constructor for Jackson. */
    public Builder() {}

    public CreateAzureStorageContainerParams.Builder resourceFields(
        CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public CreateAzureStorageContainerParams.Builder storageContainerName(
        String storageContainerName) {
      this.storageContainerName = storageContainerName;
      return this;
    }

    public CreateAzureStorageContainerParams.Builder storageAccountId(UUID storageAccountId) {
      this.storageAccountId = storageAccountId;
      return this;
    }

    /** Call the private constructor. */
    public CreateAzureStorageContainerParams build() {
      return new CreateAzureStorageContainerParams(this);
    }
  }
}
