package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.exception.UserActionableException;
import java.util.UUID;
import picocli.CommandLine;

/**
 * Command helper class for identifying a Dataproc cluster by either the workspace resource name or
 * the GCP instance name in `terra cluster` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class DataprocClusterName {
  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ArgGroup argGroup;

  public UUID toClusterResourceId() {
    Workspace workspace = Context.requireWorkspace();
    Resource resource =
        (argGroup.resourceName != null)
            ? workspace.getResource(argGroup.resourceName)
            : getClusterResourceFromId(argGroup.clusterId);

    // Ensure that the resource fetched by name is a cluster resource.
    if (resource.getResourceType().equals(Resource.Type.DATAPROC_CLUSTER)) {
      return resource.getId();
    } else {
      throw new UserActionableException(
          "Only able to use cluster commands on Dataproc resources, but specified resource is "
              + resource.getResourceType());
    }
  }

  /** Helper method to find the cluster resource in the workspace by the cluster id. */
  private Resource getClusterResourceFromId(String clusterId) {
    return Context.requireWorkspace().listResources().stream()
        .filter(
            r ->
                r.getResourceType().equals(Resource.Type.DATAPROC_CLUSTER)
                    && ((GcpDataprocCluster) r).getClusterName().name().equals(clusterId))
        .findFirst()
        .orElseThrow(() -> new UserActionableException("Cluster not found: " + clusterId));
  }

  static class ArgGroup {
    @CommandLine.Option(
        names = "--name",
        description =
            "Name of the cluster resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
    public String resourceName;

    @CommandLine.Option(names = "--cluster-id", description = "The id of the cluster.")
    public String clusterId;
  }
}
