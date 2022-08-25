package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDDataCollection;
import bio.terra.cli.serialization.userfacing.resource.UFDataCollection;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.TerraWorkspaceResource;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a data collection referenced resource. Instances of this class are
 * part of the current context or state.
 */
public class DataCollection extends Resource {
  public static final String SHORT_DESCRIPTION_KEY = "terra-workspace-short-description";
  public static final String VERSION_KEY = "terra-workspace-version";
  private static final Logger logger = LoggerFactory.getLogger(DataCollection.class);
  private final UUID dataCollectionWorkspaceUuid;

  /** Deserialize an instance of the disk format to the internal object. */
  public DataCollection(PDDataCollection configFromDisk) {
    super(configFromDisk);
    this.dataCollectionWorkspaceUuid = configFromDisk.dataCollectionWorkspaceUuid;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public DataCollection(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATA_COLLECTION;
    this.dataCollectionWorkspaceUuid =
        wsmObject.getResourceAttributes().getTerraWorkspace().getReferencedWorkspaceId();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public DataCollection(TerraWorkspaceResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATA_COLLECTION;
    this.dataCollectionWorkspaceUuid = wsmObject.getAttributes().getReferencedWorkspaceId();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFDataCollection serializeToCommand() {
    return new UFDataCollection(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDDataCollection serializeToDisk() {
    return new PDDataCollection(this);
  }

  @Override
  protected void deleteReferenced() {
    WorkspaceManagerService.fromContext()
        .deleteReferencedTerraWorkspace(Context.requireWorkspace().getUuid(), id);
  }

  @Override
  protected void deleteControlled() {
    logger.warn("Controlled data collection resource is not supported");
    throw new UnsupportedOperationException("Controlled data collection resource is not supported");
  }

  @Override
  public String resolve() {
    throw new UnsupportedOperationException(
        "Must specify which resource inside this data collection that is to be resolved");
  }

  public String resolve(String resourceName) {
    var resources = getDataCollectionWorkspace().getResources();
    return resources.stream()
        .filter(
            resource ->
                // There shouldn't be any data collection resources in a data collection workspace,
                // but filter out just in case.
                resource.getResourceType() != Type.DATA_COLLECTION
                    && resource.getName().equals(resourceName))
        .map(Resource::resolve)
        .findFirst()
        .orElseThrow(
            () ->
                new UserActionableException(
                    "Invalid path: please run "
                        + "'terra resource describe --name=[data collection name]` to check if the resource name in the data collection is specified correctly."));
  }

  public Workspace getDataCollectionWorkspace() {
    return Workspace.get(dataCollectionWorkspaceUuid, /*isDataCollectionWorkspace=*/ true);
  }

  // ====================================================
  // Property getters.

  public UUID getDataCollectionWorkspaceUuid() {
    return dataCollectionWorkspaceUuid;
  }
}
