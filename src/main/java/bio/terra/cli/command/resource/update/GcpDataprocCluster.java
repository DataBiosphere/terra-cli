package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource.Type;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import picocli.CommandLine;

@CommandLine.Command(
    name = "dataproc-cluster",
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
      description =
          "Autoscaling policy url to attach to the cluster. Format: projects/[projectId]/locations/[dataproc_region]/autoscalingPolicies/[policy_id]")
  private String autoscalingPolicy;

  @CommandLine.Option(
      names = "--graceful-decommission-timeout",
      description = "The duration to wait for graceful decommissioning to finish.")
  private String gracefulDecommissionTimeout;

  @CommandLine.Option(
      names = "--idle-delete-ttl",
      description = "Time-to-live after which the resource becomes idle and is deleted.")
  public String idleDeleteTtl;

  /** Print this command's output in text format. */
  private static void printText(UFGcpDataprocCluster returnValue) {
    OUT.println("Successfully updated GCP Dataproc cluster.");
    returnValue.print();
  }

  /** Update a GCP Dataproc cluster in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()
        && numWorkers == null
        && numSecondaryWorkers == null
        && autoscalingPolicy == null
        && gracefulDecommissionTimeout == null
        && idleDeleteTtl == null) {
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
            .numWorkers(numWorkers)
            .numSecondaryWorkers(numSecondaryWorkers)
            .autoscalingPolicyUri(autoscalingPolicy)
            .gracefulDecommissionTimeout(gracefulDecommissionTimeout)
            .idleDeleteTtl(idleDeleteTtl)
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
