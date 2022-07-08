package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.serialization.persisted.resource.PDDataSource;
import bio.terra.cli.serialization.userfacing.resource.UFDataSource;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.client.JSON;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.TerraWorkspaceResource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.impl.FutureConvertersImpl.P;

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
  public JSONObject resolve() {
    return new JSONObject();
  }

  public JSONObject resolve(String resourceName) {
    var resources = getResources();
    if (resources.isEmpty()) {
      return new JSONObject();
    }
    return resources.map(
        r ->
            r.stream()
                .filter(resource ->
                    resource.getResourceType() != Type.DATA_SOURCE
                    && resource.getName().equals(resourceName))
                .map(Resource::resolve)
                .findFirst()
                .orElse(new JSONObject())
    ).orElse(new JSONObject());
  }

  public Optional<List<Resource>> getResources() {
    try {
      return Optional.of(WorkspaceManagerService.fromContext()
          .enumerateAllResources(dataSourceWorkspaceUuid, Context.getConfig().getResourcesCacheSize())
          .stream().map(Resource::deserializeFromWsm).collect(Collectors.toList()));
    } catch(SystemException e) {
      if (e.getCause() instanceof ApiException) {
        if (((ApiException) e.getCause()).getCode() == HttpStatus.SC_FORBIDDEN) {
          return Optional.empty();
        }
      }
      throw e;
    }
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
