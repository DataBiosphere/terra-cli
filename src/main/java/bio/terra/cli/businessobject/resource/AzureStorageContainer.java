package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDAzureStorageContainer;
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

  /** No-op: referenced Azure storage containers are not supported. */
  protected void deleteReferenced() {}

  /** Delete an Azure storage container controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    // TODO
    //    WorkspaceManagerService.fromContext()
    //            WorkspaceManagerService.fromContext()
    //                    .deleteControlledBigQueryDataset(Context.requireWorkspace().getUuid(),
    // id);
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
