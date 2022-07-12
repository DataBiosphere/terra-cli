package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.serialization.persisted.resource.PDDataSource;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.TerraWorkspaceResource;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a data source referenced resource. Instances of this class are part of
 * the current context or state.
 */
public class DataSource extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(DataSource.class);

  public static final String SHORT_DESCRIPTION_KEY = "terra-workspace-short-description";
  public static final String VERSION_KEY = "terra-workspace-version";

  private final UUID dataSourceWorkspaceUuid;

  /** Deserialize an instance of the disk format to the internal object. */
  public DataSource(PDDataSource configFromDisk) {
    super(configFromDisk);
    this.dataSourceWorkspaceUuid = configFromDisk.dataSourceWorkspaceUuid;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public DataSource(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATA_SOURCE;
    this.dataSourceWorkspaceUuid =
        wsmObject.getResourceAttributes().getTerraWorkspace().getReferencedWorkspaceId();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public DataSource(TerraWorkspaceResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATA_SOURCE;
    this.dataSourceWorkspaceUuid = wsmObject.getAttributes().getReferencedWorkspaceId();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFDataSource serializeToCommand() {
    return new UFDataSource(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDDataSource serializeToDisk() {
    return new PDDataSource(this);
  }

  @Override
  protected void deleteReferenced() {
    WorkspaceManagerService.fromContext()
        .deleteReferencedTerraWorkspace(Context.requireWorkspace().getUuid(), id);
  }

  @Override
  protected void deleteControlled() {
    logger.warn("Controlled data source resource is not supported");
    throw new UnsupportedOperationException("Controlled data source resource is not supported");
  }

  /** Resolve data source. */
  // TODO(PF-1743, PF-1744): Implement
  public String resolve() {
    return "";
  }

  public Workspace getDataSourceWorkspace() {
    return Workspace.get(dataSourceWorkspaceUuid);
  }

  // ====================================================
  // Property getters.

  public UUID getDataSourceWorkspaceUuid() {
    return dataSourceWorkspaceUuid;
  }
}
