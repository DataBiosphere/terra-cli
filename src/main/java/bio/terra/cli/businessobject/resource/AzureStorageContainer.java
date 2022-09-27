package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDAzureStorageContainer;
import bio.terra.cli.serialization.userfacing.input.CreateAzureStorageContainerParams;
import bio.terra.cli.serialization.userfacing.resource.UFAzureStorageContainer;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.AzureStorageContainerResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of an Azure storage container workspace resource. Instances of this class
 * are part of the current context or state.
 */
public class AzureStorageContainer extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(BqDataset.class);

  private final UUID storageAccountId;
  private final String storageContainerName;

  /** Deserialize an instance of the disk format to the internal object. */
  public AzureStorageContainer(PDAzureStorageContainer configFromDisk) {
    super(configFromDisk);
    this.storageAccountId = configFromDisk.storageAccountId;
    this.storageContainerName = configFromDisk.storageContainerName;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public AzureStorageContainer(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AZURE_STORAGE_CONTAINER;
    this.storageAccountId =
        wsmObject.getResourceAttributes().getAzureStorageContainer().getStorageAccountId();
    this.storageContainerName =
        wsmObject.getResourceAttributes().getAzureStorageContainer().getStorageContainerName();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public AzureStorageContainer(AzureStorageContainerResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.AZURE_STORAGE_CONTAINER;
    this.storageAccountId = wsmObject.getAttributes().getStorageAccountId();
    this.storageContainerName = wsmObject.getAttributes().getStorageContainerName();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFAzureStorageContainer serializeToCommand() {
    return new UFAzureStorageContainer(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDAzureStorageContainer serializeToDisk() {
    return new PDAzureStorageContainer(this);
  }

  /**
   * Create an Azure storage container as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static AzureStorageContainer createControlled(
      CreateAzureStorageContainerParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    AzureStorageContainerResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledAzureStorageContainer(
                Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created Azure storage container: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new AzureStorageContainer(createdResource);
  }

  /** No-op: referenced Azure storage containers are not supported. */
  protected void deleteReferenced() {}

  /** Delete an Azure storage container controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledAzureStorageContainer(Context.requireWorkspace().getUuid(), id);
  }

  /** Resolve an Azure storage contaiber resource to a SAS token. */
  public String resolve() {
    return WorkspaceManagerService.fromContext()
        .getAzureStorageContainerSasToken(Context.getWorkspace().get().getUuid(), id);
  }

  // ====================================================
  // Property getters.

  public UUID getStorageAccountId() {
    return storageAccountId;
  }

  public String getStorageContainerName() {
    return storageContainerName;
  }
}
