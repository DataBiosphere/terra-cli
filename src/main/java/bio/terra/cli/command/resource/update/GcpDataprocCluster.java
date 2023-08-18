package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.DataprocClusterLifecycleConfig;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.workspace.model.ControlledDataprocClusterUpdateParameters;
import picocli.CommandLine;

@CommandLine.Command(
    name = "gcp-cluster",
    description = "Update a GCP Dataproc cluster.",
    showDefaultValues = true)
public class GcpDataprocCluster extends WsmBaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(names = "--num-workers", description = "The number of worker nodes.")
  private Integer numWorkers;

  @CommandLine.Option(
      names = "--num-secondary-workers",
      description = "The number of secondary worker nodes.")
  private Integer numSecondaryWorkers;

  @CommandLine.Option(
      names = "--autoscaling-policy",
      description = "Autoscaling policy url to attach to the cluster.")
  private String autoscalingPolicy;

  @CommandLine.ArgGroup(
      exclusive = false,
      multiplicity = "0..1",
      heading = "Lifecycle configurations")
  DataprocClusterLifecycleConfig lifeCycleConfig = new DataprocClusterLifecycleConfig();

  /** Print this command's output in text format. */
  private static void printText(UFGcpDataprocCluster returnValue) {
    OUT.println("Successfully updated GCP Dataproc cluster.");
    returnValue.print();
  }

  @CommandLine.Option(
      names = "--graceful-decommission-timeout",
      description = "The duration to wait for graceful decommissioning to finish.")
  private String gracefulDecommissionTimeout;

  /** Update a GCP Dataproc cluster in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && numWorkers == null
        && numSecondaryWorkers == null
        && autoscalingPolicy == null
        && gracefulDecommissionTimeout == null) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.GcpDataprocCluster resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Type.DATAPROC_CLUSTER);

    resource.updateControlled(
        new UpdateControlledGcpDataprocClusterParams.Builder()
            .resourceFields(resourceUpdateOptions.populateMetadataFields().build())
            .clusterUpdateParams(
                new ControlledDataprocClusterUpdateParameters()
                    .numPrimaryWorkers(numWorkers)
                    .numSecondaryWorkers(numSecondaryWorkers)
                    .autoscalingPolicy(autoscalingPolicy)
                    .gracefulDecommissionTimeout(gracefulDecommissionTimeout))
            .build());

    // re-load the resource so we display all properties with up-to-date values
    resource =
        Context.requireWorkspace()
            .getResource(resource.getName())
            .castToType(Type.DATAPROC_CLUSTER);
    formatOption.printReturnValue(
        new UFGcpDataprocCluster(resource), GcpDataprocCluster::printText);
  }
}
